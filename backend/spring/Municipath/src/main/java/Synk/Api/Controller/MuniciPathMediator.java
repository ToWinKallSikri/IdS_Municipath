package Synk.Api.Controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import Synk.Api.Controller.Analysis.AnalysisHandler;
import Synk.Api.Controller.City.CityHandler;
import Synk.Api.Controller.Feedback.FeedbackHandler;
import Synk.Api.Controller.Group.GroupHandler;
import Synk.Api.Controller.Pending.PendingHandler;
import Synk.Api.Controller.Post.PointHandler;
import Synk.Api.Controller.SavedContent.SavedContentHandler;
import Synk.Api.Controller.User.UserHandler;
import Synk.Api.Model.MetaData;
import Synk.Api.Model.City.City;
import Synk.Api.Model.City.Role.Role;
import Synk.Api.Model.Feedback.Score;
import Synk.Api.Model.Pending.PendingRequest;
import Synk.Api.Model.Post.Position;
import Synk.Api.Model.Post.Post;
import Synk.Api.Model.Post.PostType;
import Synk.Api.View.ViewModel.ProtoGroup;
import Synk.Api.View.ViewModel.ProtoPost;
import jakarta.annotation.PostConstruct;

/**
 * classe che implementa il pattern
 * mediator per gli handler del progetto
 */
@Service
public class MuniciPathMediator {
	
	private PointHandler point;
	private UserHandler user;
	private CityHandler city;
	private GroupHandler group;
	private PendingHandler pending;
	private FeedbackHandler feedback;
	private IdentifierManager idManager;
	private SavedContentHandler saved;
	private AnalysisHandler analysis;
	
	MuniciPathMediator(PointHandler point, UserHandler user, CityHandler city,
						GroupHandler group, PendingHandler pending, FeedbackHandler feedback,
						SavedContentHandler saved, AnalysisHandler analysis){
		this.point = point;
		this.user = user;
		this.city = city;
		this.group = group;
		this.pending = pending;
		this.feedback = feedback;
		this.saved = saved;
		this.analysis = analysis;
		this.idManager = new IdentifierManager();
	}
	
	@PostConstruct
	public void inject() {
		this.point.setMediator(this);
		this.city.setMediator(this);
		this.feedback.setMediator(this);
		this.group.setMediator(this);
		this.pending.setMediator(this);
		this.user.setMediator(this);
		this.saved.setMediator(this);
		this.analysis.setMediator(this);
	}
	
	/**
	 * controlla se un dato autore può postare in un dato comune
	 * @param cityId id del comune
	 * @param author username dell'autore
	 * @return true se puo' postare, false altrimenti
	 */
	public boolean isAuthorizedToPost(String cityId, String author) {
		return this.getRoleLevel(cityId, author) > 1;
	}
	
	/**
	 * controlla se un dato autore può pubblicare in un dato comune
	 * @param cityId id del comune
	 * @param author username dell'autore
	 * @return true se puo' pubblicare, false altrimenti
	 */
	public boolean canPublish(String cityId, String author) {
		return this.getRoleLevel(cityId, author) > 2;
	}
	
	/**
	 * prende il ruolo e ne ricava un numero 
	 * che rapprensenta quanto
	 * è autorizzato in quel comune.
	 * @param cityId
	 * @param author
	 * @return un intero che rappresenta il grado del suo permesso.
	 */
	public int getRoleLevel(String cityId, String author) {
		Role role = this.city.getRole(author, cityId);
		switch(role) {
			case CURATOR: 
				return 5;
			case MODERATOR: 
				return 4;
			case CONTR_AUTH: 
				return 3;
			case CONTR_NOT_AUTH: 
				return 2;
			case TOURIST: 
				return 1;
			default:
				return 0;
		}
	}
	

	/**
	 * metodo per vedere se in un dato comune, un utente e' moderatore o curatore
	 * @param cityId
	 * @param author
	 * @return
	 */
	public boolean isTheStaff(String cityId, String author) {
		return this.getRoleLevel(cityId, author) > 3;
	}
	
	/**
	 * metodo per aggiungere un pending di creazione al pending handler
	 * @param id id del contenuto da inserire
	 */
	public void addPending(String id) {
		this.pending.addRequest(id);
	}
	
	/**
	 * metodo per aggiungere un pending di modifica post al pending handler
	 * @param postId id del post
	 * @param data dati del post
	 */
	public void addPostPending(String postId, ProtoPost data) {
		this.pending.addPostRequest(postId, data);
	}
	
	/**
	 * metodo per aggiungere un pending di modifica group al pending handler
	 * @param groupId id del gruppo
	 * @param data dati del group
	 */
	public void addGroupPending(String groupId, ProtoGroup data) {
		this.pending.addGroupRequest(groupId, data);
	}
	
	/**
	 * metodo per rimuovere tutti i gruppi di un dato comune
	 * @param cityId id del comune
	 */
	public void removeAllCityGroups(String cityId) {
		this.group.removeAllFromCity(cityId);
		
	}
	
	/**
	 * metodo per rimuovere da tutti i gruppi un certo post 
	 * @param post post id da rimuovere
	 */
	public void removeFromAllGroups(String post) {
		this.group.removeFromAll(post);
	}
	
	/**
	 * metodo per ottenere un comune dall'id
	 * @param cityID id del comune
	 * @return comune ricercato se esiste, null altrimenti
	 */
	public City getCity(String cityID) {
		return this.city.getCity(cityID);
	}
	
	/**
	 * metodo per associare un curatore ad un comune
	 * @param curator username del nuovo curatore
	 * @param id id del comune
	 * @return true se e' stato associato, false altrimenti
	 */
	public boolean matchCurator(String curator, String id) {
		return this.user.matchCurator(curator, id);
	}

	/**
	 * metodo per inizializzare il point handler 
	 * rispetto ad un nuovo comune
	 * @param id id del nuovo comune
	 * @param cityName nome del comune
	 * @param curator curatore del comune
	 * @param pos posizione del comune
	 */
	public void createPostForNewCity(String id, String cityName, String curator, Position pos) {
		ProtoPost post = new ProtoPost();
		post.setMultimediaData(new ArrayList<>());
		post.setPersistence(true);
		post.setTitle("Comune di "+cityName);
		post.setMultimediaData(new ArrayList<>());
		post.setType(PostType.INSTITUTIONAL);
		post.setText("");
        this.point.createPost(curator, pos, id, post);
	}
	
	/**
	 * metodo per cambiare curatore di un comune
	 * @param curator nuovo curatore
	 * @param cityId id del comune
	 * @return true se il curatore e' stato cambiato, false altrimenti
	 */
	public boolean changeCurator(String curator, String cityId) {
		return this.user.changeCurator(curator, cityId);
	}
	
	/**
	 * metodo chiamato da city handler per eliminare un comune
	 * si assicura che anche l'user hander e point handler vengano aggiornati
	 * @param cityId id del comune da eliminare
	 */
	public void deleteCity(String cityId) {
    	this.user.discreditCurator(cityId);
    	this.point.deleteCityPoints(cityId);
	}
	
	public void updateCityPrime(City city, Position oldPos) {
		this.point.updatePrime(city, oldPos);
	}
	
	/**
	 * metodo che controlla se un dato username esiste
	 * @param username username da controllare
	 * @return true se esiste, false altrimenti
	 */
	public boolean usernameExists(String username) {
		return this.user.usernameExists(username);
	}
	
	
	/**
	 * metodo chiamato dal group handler per controllare se
	 * un insieme di id di post corrispondono ad un uguale numero
	 * di post esistenti
	 * @param postIds id dei post da controllare
	 * @return lista dei post se tutti esistono, null altrimenti
	 */
	public List<Post> getPostsIfAllExists(List<String> postIds) {
		return this.point.getPostsIfAllExists(postIds);
	}
	
	/**
	 * metodo chiamato dal point handler per aggiornare
	 * un post quando viene visualizzato
	 * @param post post visualizzato
	 * @param username visualizzatore
	 * @return lista di gruppi di cui il post fa parte
	 */
	public List<String> viewGroupFrom(Post post, String username) {
		return this.group.viewGroupFrom(post.getId(), username);
	}
	
	/**
	 * metodo per ottenere 
	 * l'autore di un post o di un
	 * gruppo tramite l'id
	 * @param pendingId id del contenuto
	 * @return l'autore se esiste, false altrimenti
	 */
	public String getAuthor(String contentId) {
		return this.getMetaData(contentId).getAuthor();
	}
	

	/**
	 * metodo per controllare se un utente
	 * è autore di contenuto
	 * @param username nome utente
	 * @param contentId contenuto
	 * @return true se è il vero autore o se
	 * appartiene ad un comune di cui possiede
	 * i privilegi di moderatore o curatore
	 */
	public boolean isAuthor(String username, String contentId) {
		MetaData content = this.getMetaData(contentId);
		if(content.isOfCity())
			return this.getRoleLevel(idManager.getCityId(contentId), username) > 3;
		else return content.getAuthor().equals(username);
	}
	
	
	/**
	 * metodo per gestire una richiesta in pending di gruppo approvata
	 * @param request richiesta da gestire
	 */
	public void manageGroupRequest(PendingRequest request) {
		if(request.isNew())
			this.group.approveGroup(request.getId());
		else this.group.editGroup(request);
	}
	
	/**
	 * metodo per gestire una richiesta di pensing di post approvata
	 * @param request richiesta da gestire
	 */
	public void managePostRequest(PendingRequest request) {
		if(request.isNew())
			this.point.approvePost(request.getId());
		else this.point.editPost(request);
	}
	
	/**
	 * metodo per eliminare un group in pending
	 * @param pendingId id del gruppo
	 */
	public void deletePendingGroup(String pendingId) {
		this.group.removeGroup(pendingId);
		
	}
	
	/**
	 * metodo per eliminare un post in pending
	 * @param pendingId id del post
	 */
	public void deletePendingPost(String pendingId) {
		this.point.deletePost(pendingId);
	}
	
	/**
	 * metodo per ricevere il voto di
	 * contenuto
	 * @param id id del contenuto
	 * @return voto del contenuto
	 */
	public Score getVoteOf(String id) {
		return this.feedback.getFeedback(id);
	}
	
	/**
	 * metodo per controllare se un dato contenuto
	 * esiste o meno
	 * @param contentId id del contenuto
	 * @return true se esiste, false altrimenti
	 */
	public boolean contentExist(String contentId) {
		return getMetaData(contentId) != null;
	}
	
	/**
	 * metodo per controllare se un comune
	 * esiste o meno
	 * @param cityId id del comune
	 * @return true se esiste, false altrimenti
	 */
	public boolean checkCity(String cityId) {
		return this.city.getCity(cityId) != null;
	}
	
	/**
	 * metodo per ottenre un metadato, cioè un
	 * contenuto astratto con meno informazioni
	 * @param contentId id del contenuto
	 * @return il contenuto ricercato
	 */
	public MetaData getMetaData(String contentId) {
		if(this.idManager.isGroup(contentId))
			return this.group.viewGroup(contentId);
		return this.point.getPost(contentId);
	}
	
	/**
	 * metodo per ottenre gli username che hanno
	 * salvato un dato contenuto
	 * viene usato per vedere i "partecipanti 
	 * ad un evento".
	 * @param contentId id del contenuto
	 * @return username che lo hanno salvato
	 */
    public List<String> getPartecipants(String contentId) {
        return this.saved.getPartecipants(contentId);
    }
    
    /**
     * metodo per inviare una notifica
     * @param author autore della notifica
     * @param contentId id del contenuto
     * @param message messaggio da inviare
     * @param reciver destinatario
     */
    public void send(String author, String contentId, String message, String reciver) {
		this.user.notify(author, message, contentId, reciver);
	}
    
    /**
     * metodo per ottenere il nome di un comune.
     * @param cityId id del comune.
     * @return nome del comune se esiste, altrimenti null.
     */
	public String getNameOfCity(String cityId) {
		City city = this.city.getCity(cityId);
		return city == null ? null : city.getName();
	}
	
	/**
	 * metodo per notificare la creazione
	 * a tutti i follower
	 * @param data contenuto da notificare
	 */
	public void notifyCreation(MetaData data) {
		this.user.notifyCreation(data);
	}
	
	/**
	 * metodo per rimuovere tutti i salvataggi
	 * di feedback e di contenuti di un contenuto
	 * destinato alla eliminazione
	 * @param contentId id del contenuto
	 */
	public void removeAllDataOf(String contentId) {
		this.feedback.removeAllFeedbackOf(contentId);
		this.saved.removeAllFromContent(contentId);
	}
	
	/**
	 * metodo per rimuovere tutti i salvataggi
	 * di contenuti appartenenti ad un account
	 * destinato alla eliminazione
	 * @param username nome utente
	 */
	public void removeAllSaveContentOf(String username) {
		this.saved.removeAllFromUser(username);
	}
	
	/**
	 * metodo per ottenere tutti 
	 * i contenuti in una certa
	 * distanza temporale
	 * @param cityId id del comune da controllare
	 * @param months numero di mesi in cui scavare
	 * @param onlyUsers se ignorare i cotenuti dello staff
	 * @return i contenuti richiesti
	 */
	public List<MetaData> getDataForAnalysis(String cityId, int months, boolean onlyUsers) {
		LocalDateTime from = LocalDateTime.now().minusMonths(months);
		List<MetaData> list = new ArrayList<>();
		list.addAll(this.point.getPosts(cityId, from));
		list.addAll(this.group.viewGroups(cityId, from));
		if(onlyUsers)
			list = new ArrayList<>(list.stream().parallel().filter(d -> !d.isOfCity()).toList());
		return list;
	}
	
}
