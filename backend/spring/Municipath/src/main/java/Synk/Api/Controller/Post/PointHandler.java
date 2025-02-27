package Synk.Api.Controller.Post;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import Synk.Api.Controller.AuthorProvider;
import Synk.Api.Controller.IdentifierManager;
import Synk.Api.Controller.MuniciPathMediator;
import Synk.Api.Controller.Post.Contribute.ContributeHandler;
import Synk.Api.Controller.WeatherService.WeatherForecastProxy;
import Synk.Api.Controller.WeatherService.WeatherService;
import Synk.Api.Model.City.City;
import Synk.Api.Model.Pending.PendingRequest;
import Synk.Api.Model.Post.Point;
import Synk.Api.Model.Post.Position;
import Synk.Api.Model.Post.Post;
import Synk.Api.Model.Post.PostPointRepository;
import Synk.Api.Model.Post.PostRepository;
import Synk.Api.Model.Post.PostType;
import Synk.Api.Model.Post.Contribute.Contribute;
import Synk.Api.View.ViewModel.ProtoPost;

@Service
public class PointHandler implements AuthorProvider {

	private final Integer CONTR_NOT_AUTH_LEVEL = 2;
	private final Integer CONTR_AUTH_LEVEL = 3;
	/**
	 * il servizio meteo e il mediator,
	 * che collega la classe con
	 * gli altri handler
	 */
    private WeatherService weather;
    private MuniciPathMediator mediator;
    private IdentifierManager idManager;
    private PostCreator normalCreator, eventCreator, contestCreator;
    private PostValidator validator;
    
    /**
     * beans iniettati per la persistenza
     */
	@Autowired
	private PostPointRepository pointRepository;
	@Autowired
    private ContributeHandler contributes;
	@Autowired
	private PostRepository postRepository;
    
	/**
	 * costruttore della classe. inizializza la mappa
	 * per le ricerche rapide e il servizio per le
	 * previsioni meteo.
	 */
    public PointHandler() {
        weather = new WeatherForecastProxy();
        idManager = new IdentifierManager();
        normalCreator = new NormalPostCreator();
        eventCreator = new EventPostCreator();
        contestCreator = new ContestPostCreator();
        validator = new PostValidator();
    }
    
    /**
     * Inserisce il mediator nella classe
     * @param mediator mediator da inserire
     */
    public void setMediator(MuniciPathMediator mediator) {
        this.mediator = mediator;
    }

    
    /**
     * metodo per creare un post
     * @param author autore del post
     * @param pos posizione del post
     * @param cityId id del comune
     * @param post dati del post
     * @return true se l'operazione e' andata a buon fine, false altrimenti
     */
	public boolean createPost(String author, Position pos, String cityId, ProtoPost post) {
		if(author == null || pos == null || cityId == null || post == null)
    		return false;
		int level = this.mediator.getRoleLevel(cityId, author);
    	if(level < CONTR_NOT_AUTH_LEVEL)
    		return false;
		PostCreator creator = buildingPost(author, pos, level, post);
    	if(!creator.correctPost())
    		return false;
        Point point = this.pointRepository.findById(cityId+"."+pos);
        if(point == null)
        	point = new Point(pos, cityId);
    	creator.setIds(point.getNewPostId(), point.getPointId(), cityId);
    	Post newPost = creator.createPost();
        point.getPosts().add(newPost);
        postRepository.save(newPost);
    	pointRepository.save(point);
        if(level == CONTR_NOT_AUTH_LEVEL)
        	this.mediator.addPending(newPost.getId());
        else this.mediator.notifyCreation(newPost);
        return true;
	}
	
	

	/**
	 * metodo che inserisce le corrette informazioni dentro ad un post
	 * @param author autore del post
	 * @param pos posizione del post
	 * @param level livello di autorizzazione del post
	 * @param post dati del post
	 * @return creator pronto a produrre il post
	 */
	private PostCreator buildingPost(String author, Position pos, int level, ProtoPost post) {
		PostCreator creator = getRightCreator(post.getType());
		creator.initializePost();
    	creator.setData(post.getTitle(), post.getText(), post.getMultimediaData());
    	creator.setDetails(author, pos, level > CONTR_AUTH_LEVEL, post.getType());
    	creator.setSpecialDetails(level > CONTR_NOT_AUTH_LEVEL, post.getStartTime(), post.getEndTime(), post.isPersistence());
    	return creator;
	}
	
	/**
	 * metodo per scegliere il creator giusto per un post
	 * @param type tipo di post
	 * @return creator adatto al post
	 */
	private PostCreator getRightCreator(PostType type) {
		return type == PostType.EVENT ? this.eventCreator : type == PostType.CONTEST ?
				this.contestCreator : this.normalCreator;
	}
    
    /**
     * Metodo per la modifica di un post da parte dell'autore
     * @param postId id del post
     * @param author autore del post
     * @param cityId citta' del post
     * @param data dati aggiornati del post
     */
	public boolean editPost(String postId, String author, String cityId, ProtoPost data) {
		if(postId == null || author == null || cityId == null ||  data == null)
    		return false;
    	PostCreator creator = getRightCreator(data.getType());
    	boolean published = this.mediator.canPublish(cityId, author);
    	if(!validator.correctPost(creator, published, data.getStartTime(), data.getEndTime(), data.isPersistence()))
    		return false;
    	Post post = this.getPost(postId);
		if(post == null || (!post.getAuthor().equals(author)) || isPrime(post))
    		return false;
    	if(published) {
            checkContest(post.getId(), post.getType(), data.getType());
    		post.updateInfo(data);
            postRepository.save(post);
    	}
    	else this.mediator.addPostPending(postId, data);
        return true;
    }
	
	/**
	 * Metodo per la modifica di un post da parte del comune
	 * @param postId id del post da modificare
	 * @param data nuovi dati del post
	 * @return true se la modifica e' andata a buon fine, false altrimenti
	 */
	public boolean editPost(String postId, ProtoPost data) {
		if(postId == null || data == null)
    		return false;
		PostCreator creator = getRightCreator(data.getType());
    	if(!validator.correctPost(creator, true, data.getStartTime(), data.getEndTime(), data.isPersistence()))
    		return false;
    	Post post = this.getPost(postId);
    	if(post == null || isPrime(post))
    		return false;
        checkContest(post.getId(), post.getType(), data.getType());
    	post.updateInfo(data);
        postRepository.save(post);
        String cityName = this.mediator.getNameOfCity(post.getCityId());
        this.mediator.send(cityName, post.getId(), "Il tuo post è stato modificato dal comune.", post.getAuthor());
        return true;
    }
    
	/**
	 * metodo per la modifica di un post da parte di una richiesta
	 * prima in pending e ora accettata. viene usato dal mediator
	 * chiamato dal PendingHandler
	 * @param request richiesta di modifica accettata
	 */
    public void editPost(PendingRequest request) {
    	if(request == null)
    		return;
    	Post post = getPost(request.getId());
        checkContest(post.getId(), post.getType(), request.getType());
    	post.updateInfo(request);
        postRepository.save(post);
    }
    

    /**
     * metodo per aggiornare il prime nel caso del cambio di posizione del comune
     * @param city nuovi dati cambiati del comune
     */
	public void updatePrime(City city, Position oldPos) {
		Post post = this.postRepository.findById(city.getId()+"."+oldPos+".0").get();
		Point point = this.pointRepository.findById(post.getPointId());
		this.pointRepository.deleteById(point.getPointId());
		this.postRepository.deleteById(post.getId());
		point.setPos(city.getPos());
		String pid = city.getId()+"."+city.getPos();
		point.setPointId(pid);
		post.setPos(city.getPos());
		post.setAuthor(city.getCurator());
		post.setTitle("Comune di "+city.getName());
		post.setPointId(pid);
		post.setId(pid+".0");
		postRepository.save(post);
		pointRepository.save(point);
	}
	
	/**
	 * metodo privato per controllare se una modifica di un post
	 * lo porti eventalmente a cambiare le sue dinamiche come contest
	 * @param id id del post
	 * @param type vecchio tipo del post
	 * @param newType nuovo tipo del post
	 */
	private void checkContest(String id, PostType type, PostType newType) {
		if (type == PostType.CONTEST && newType != PostType.CONTEST)
				this.contributes.removeContest(id);
	}
    
    /**
     * metodo che ritorna tutti i punti di una citta'
     * funziona in base all'username perche' nasconde
     * quelli che non dovrebbero essere visti dall'utente in questione
     * @param cityId id del comune
     * @param username nome utente
     * @return lista di punti
     */
    public List<Point> getPoints (String cityId, String username) {
    	if(cityId == null)
    		return null;
        return this.pointRepository.findByCityId(cityId).stream()
        		  .filter( p -> p.getPosts().stream().anyMatch(po -> toShow(po, username))).toList();
    }
    
    /**
     * aggiorna le informazioni di un dato post
     * con il meteo attuale e i gruppi di cui fa parte
     * l'username serve per controllare la visibilita' dei gruppi
     * @param post post da aggiornare
     * @param username username per la visualizzazione
     * @return post aggiornato
     */
    private Post updatePost(Post post, String username) {
    	post.setMeteo(this.weather.getWeather(post.getPos(), post.getMeteoDate()));
    	post.setGroups(this.mediator.viewGroupFrom(post, username));
    	post.setVote(this.mediator.getVoteOf(post.getId()));
    	return post;
    }
    
    /**
     * elimina tutti i punti e i relativi posts
     * e gruppi di una citta'
     * viene chiamato dal mediator, chiamato a sua volta dal
     * CityHandler
     * @param cityId id del comune da cancellare
     */
    public void deleteCityPoints (String cityId) {
    	if(cityId == null)
    		return;
        List<Point> ps = this.pointRepository.findByCityId(cityId);
        this.pointRepository.deleteAll(ps);
        ps.stream().map(p -> p.getPosts())
        	.forEach(p -> {
        		p.forEach(po -> this.mediator.removeAllDataOf(po.getId()));
        		this.postRepository.deleteAll(p);
        		});
        this.mediator.removeAllCityGroups(cityId);
    }
    
    /**
     * ritorna la lista dei post presenti attualmente
     * in un dato punto. l'username serve per verificare se
     * un dato utente ha la possibilita' di vedere un post
     * o meno.
     * @param pointId id del punto
     * @param username nome utente del visualizzatore
     * @return lista di post da vedere
     */
    public List<Post> viewPosts (String pointId, String username) {
    	if(pointId == null)
    		return null;
    	List<Post> posts = this.postRepository.findByPointId(pointId);
        return posts.stream().filter(p -> toShow(p, username)).toList();
    }
    
    /**
     * metodo privato che controlla se un contenuto debba essere visto
     * o meno da un dato utente
     * @param post post da verificare
     * @param username nome utente
     * @return true se il post e' visibilie, false altrimenti
     */
    private boolean toShow(Post post, String username) {
    	if(post.getAuthor().equals(username))
    		return true;
    	if(!post.isPublished())
    		return false;
    	return post.getType() != PostType.EVENT || post.getEndTime().isAfter((LocalDateTime.now()));
    }
    
    /**
     * metodo per ottenere un post. il metodo e' destinato
     * ad essere usato dal controller
     * @param postId id del post
     * @param username nome utente
     * @return post ricercato
     */
    public Post getPost(String postId, String username) {
    	if(postId == null)
    		return null;
    	Post post = postRepository.findById(postId).orElse(null);
    	if(post == null)
    		return null;
    	post.addOneView();
    	this.postRepository.save(post);
    	return updatePost(post, username);
    }
    
    /**
     * metodo per ottenere un post. il metodo e' destinato
     * ad essere usato dalla logica interna del model
     * @param postId id del post
     * @return post ricercato
     */
    public Post getPost(String postId) {
    	if(postId == null)
    		return null;
    	Post post = postRepository.findById(postId).orElse(null);
    	return post == null ? null : updatePost(post, null);
    }
    
    /**
     * metodo per elimanare un post. e' destinato 
     * all'uso dell'autore
     * @param postId id del post
     * @param author autore del post
     * @return true se e' stato eliminato, false altrimenti
     */
    public boolean deletePost (String postId, String author) {
    	if(postId == null || author == null)
    		return false;
    	Post post = getPost(postId);
    	if(!(post != null && post.getAuthor().equals(author)))
    		return false;
    	if(isPrime(post))
    		return false;
    	return deletePost(post);
    }
    
    /**
     * metodo per eliminare un post. e' destinato
     * all'uso del comune
     * @param postId postId da eliminare
     * @return true se e' stato eliminato, false altrimenti
     */
    public boolean deletePost (String postId) {
    	if(postId == null)
    		return false;
    	Post post = getPost(postId);
    	if(post == null || isPrime(post))
    		return false;
        String cityName = this.mediator.getNameOfCity(post.getCityId());
        this.mediator.send(cityName, post.getId(), "Il tuo post è stato eliminato dal comune.", post.getAuthor());
    	return deletePost(post);
    }
    
    /**
     * metodo privato con la logica dell'eliminazione
     * @param post post da eliminare
     * @return true se il post e' stato eliminato. false altrimenti
     */
    private boolean deletePost(Post post) {
    	if(post.getType() == PostType.CONTEST)
    		this.contributes.removeContest(post.getId());
    	Point point = this.pointRepository.findById(post.getPointId());
    	this.postRepository.save(post);
    	point.getPosts().remove(post);
    	if(point.getPosts().isEmpty()) {
        	this.pointRepository.delete(point);
    	} else this.pointRepository.save(point);
    	this.postRepository.delete(post);
    	this.mediator.removeFromAllGroups(post.getId());
    	this.mediator.removeAllDataOf(post.getId());
    	return true;
    }
    
    /**
     * metodo privato per controllare se un post e' il primo del comune
     * non puo' essere modificato e puo' essere eliminato solo 
     * cancellando l'intera citta'.
     * @param post post da controllare
     * @return true se il post e' prime, false altrimenti
     */
	private boolean isPrime(Post post) {
		City city = this.mediator.getCity(post.getCityId());
		return post.getId().equals(city.getId()+"."+city.getPos()+".0");
	}
	
	/**
	 * data una lista di id di post, ritorna tutti i post a
	 * cui corripondono gli id
	 * @param postIds id dei post
	 * @return lista di post ricercati
	 */
	public List<Post> getPosts(List<String> postIds){
		if(postIds == null)
    		return null;
		String cityId = idManager.getCityId(postIds.get(0));
		return this.pointRepository.findByCityId(cityId).stream()
				.map(p -> p.getPosts()).flatMap(List::stream).
				filter(post -> postIds.contains(post.getId()))
			    .collect(Collectors.toList());
	}
	
	/**
	 * Metodo per le analisi dei dati.
	 * Raccoglie tutti i post pubblicati 
	 * dopo una certa data e in un certo comune
	 * @param cityId luogo di pubblicazione
	 * @param from data di partenza
	 * @return dati per le analisi
	 */
	public List<Post> getPosts(String cityId, LocalDateTime from) {
		if(cityId == null || from == null)
			return null;
		return this.postRepository.findByCityId(cityId).stream()
				.filter(p -> p.getPublicationTime().isAfter(from))
				.map(p -> {
					p.setVote(this.mediator.getVoteOf(p.getId())); 
					return p;}).toList();
	}
	
	/**
	 * data una lista di postIds, ritorna una lista di tutti
	 * i post corrispondenti
	 * @param postIds ids da ricercare
	 * @return una lista contenente tutti i post se tutti gli id ricercati
	 * esistono, altrimenti null.
	 */
	public List<Post> getPostsIfAllExists(List<String> postIds) {
		if(postIds == null)
			return null;
		List<Post> posts = getPosts(postIds);
		return posts.size() == postIds.size() ? posts : null;
	}
	
	/**
	 * metodo per approvare un post.
	 * viene chiamato dal moderator, che a sua volta
	 * viene chiamato dal PendingHandler
	 * @param postId id del post da approvare
	 * @return se il post e' stato pubblicato true, false altrimenti.
	 */
	public boolean approvePost(String postId) {
		if(postId == null)
			return false;
		Post post = getPost(postId);
		if(post == null || post.isPublished())
			return false;
		post.setPublished(true);
		post.setPublicationTime(LocalDateTime.now());
        postRepository.save(post);
        this.mediator.notifyCreation(post);
		return true;
	}
	
	/**
	 * metodo per ottenere tutti i contributi di un dato post
	 * @param username autore del post
	 * @param postId id del post
	 * @return contributi del post se l'autore e' corretto, null altrimenti
	 */
	public List<Contribute> getContributes(String username, String postId){
		if(username == null || postId == null)
			return null;
		Post post = getPost(postId);
		if(post == null || post.getType() != PostType.CONTEST || (!post.getAuthor().equals(username)))
			return null;
		return this.contributes.getContributes(postId);
	}
	
	/**
	 * metodo per ottenere tutti i contributi di un dato post
	 * @param postId id del post
	 * @return contributi del post se esso esiste, null altrimenti
	 */
	public List<Contribute> getContributes(String postId){
		if(postId == null)
			return null;
		Post post = getPost(postId);
		if(post == null || post.getType() != PostType.CONTEST)
			return null;
		return this.contributes.getContributes(postId);
	}
	
	/**
	 * metodo per aggiungere contenuti ad un contest
	 * @param contestAuthor autore del contest
	 * @param contestId id del contest
	 * @param content contenuti da aggiungere
	 * @return true se i contenuti sono stati aggiunti, false altrimenti
	 */
	public boolean addContentToContest(String contestAuthor, String contestId, List<String> content) {
		if(contestAuthor == null || contestId == null || content == null)
    		return false;
		if(!this.mediator.usernameExists(contestAuthor))
			return false;
		Post contest = getPost(contestId);
		if(contest == null || contest.getType() != PostType.CONTEST || contest.getEndTime().isBefore(LocalDateTime.now()))
			return false;
		return this.contributes.addContributeToContest(contestAuthor, contestId, content);
	}
	
	/**
	 * metodo per dichiarare il vincitore di un contest
	 * @param author autore del contest
	 * @param contestId id del contest
	 * @param winnerId id del vincitore
	 * @return true se la dichiarazione e' andata a buon fine, false altrimenti
	 */
	public boolean declareWinner(String author, String contestId, String winnerId) {
		if(author == null || contestId == null || winnerId == null)
			return false;
		Post post = getPost(contestId);
		if(post == null || post.getType() != PostType.CONTEST 
				|| (!post.getAuthor().equals(author)) || post.getEndTime().isAfter(LocalDateTime.now()))
			return false;
		List<String> winnercontent = this.contributes.declareWinner(contestId, winnerId);
		if(winnercontent == null)
			return false;
		showWinner(post.getId(), post.getTitle(), winnercontent, winnerId);
		Point point = pointRepository.findById(post.getPointId());
		this.pointRepository.save(point);
		return true;
	}
	
	private void showWinner(String postId, String title, List<String> content, String winnerId) {
		ProtoPost data = new ProtoPost();
		data.setTitle(title);
		data.setText("Vincitore: "+winnerId);
		data.setPersistence(true);
		data.setType(PostType.SOCIAL);
		data.setMultimediaData(content);
		PendingRequest edit = new PendingRequest(postId, data);
		editPost(edit);
	}
	
	/**
	 * metodo che si assicura che tutti gli eventi finiti
	 * che non hanno persistenza vengano rimossi dal sistema.
	 */
	public void checkEndingPosts() {
		LocalDateTime date = LocalDateTime.now();
		List<Post> toDelete = new ArrayList<>();
		pointRepository.findAll().stream().parallel()
				.map(poi -> poi.getPosts()).forEach( l -> l.stream()
				.filter(pos -> ! pos.isPersistence())
				.filter(pos -> pos.getType() == PostType.EVENT)
				.forEach(pos -> {
					if(pos.getEndTime().isBefore(date))
						toDelete.add(pos); }));
		for(Post post : toDelete) {
			deletePost(post.getId());
		}
	}
	
	/**
	 * Ottieni autore del post. viene usato dal moderator per
	 * sapere a chi mandare l'esito di un pending.
	 * @param pendingId id del contenuto.
	 * @return autore del contenuto
	 */
	public String getAuthor(String pendingId) {
		Post post = this.getPost(pendingId);
		return post == null ? null : post.getAuthor();
	}
    
}
