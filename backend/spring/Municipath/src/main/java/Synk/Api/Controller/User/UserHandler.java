package Synk.Api.Controller.User;


import java.util.List;
import java.util.stream.StreamSupport;


import Synk.Api.Controller.MuniciPathMediator;
import Synk.Api.Model.User.NotificationHandler;
import Synk.Api.Model.User.User;
import Synk.Api.Model.User.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserHandler {

	/**
	 * Oggetto NotificatioHandler, utilizzato per gestire le notifiche
	 */
    private NotificationHandler notifications;

	/**
	 * Oggetto mediator, utilizzato per far comunicare i vari handler fra di loro
	 */
    private MuniciPathMediator mediator;

	/**
	 * Oggetto encoder, utilizzato per criptare le password
	 */
	private BCryptPasswordEncoder encoder;

	/**
	 * Oggetto userRepository, utilizzato per gestire la JPA
	 */
	@Autowired
	private UserRepository userRepository;

	/**
	 * Costruttore della classe UserHandler, per gli oggetti "notifications" e "encoder"
	 */
    public UserHandler() {
        this.notifications = new NotificationHandler();
        this.encoder = new BCryptPasswordEncoder();
    }

	/**
	 * Metodo setter per l'oggetto mediator
	 * @param mediator, il mediator da settare
	 */
	public void setMediator(MuniciPathMediator mediator) {
		this.mediator = mediator;
	}

	/**
	 * Metodo per creare un utente
	 * @param username, lo username dell'utente
	 * @param password, la password dell'utente
	 * @return true se l'utente è stato creato, false altrimenti
	 */
	public boolean addUser(String username, String password) {
		if(usernameExists(username))
			return false;
		password = encoder.encode(password);
		this.userRepository.save(new User(username, password, false, false));
		return true;
	}

	/**
	 * Metodo per rimuovere un utente
	 * @param username, lo username dell'utente da rimuovere
	 * @return true se l'utente è stato rimosso, false altrimenti
	 */
	public boolean removeUser(String username) {
		if(!this.usernameExists(username)) 
			return false;
		this.userRepository.deleteById(username);
		return true;
	}

	/**
	 * Metodo per controllare se ci sono match della password in chiaro con quella criptata, per un determinato utente
	 * @param username, lo username dell'utente
	 * @param password, la password dell'utente
	 * @return true se la password corrisponde, false altrimenti
	 */
	public boolean isThePassword(String username, String password) {
		User user = getConvalidatedUser(username);
		if(user == null) 
			return false;
		return this.encoder.matches(password, user.getPassword());
	}
	
	public boolean changePassowrd(String username, String password) {
		User user = getConvalidatedUser(username);
		if(user == null) 
			return false;
		password = encoder.encode(password);
		user.setPassword(password);
		this.userRepository.save(user);
		return true;
	}
	
	public boolean userValidation(String username) {
		User user = getUser(username);
		if(user == null || user.isConvalidated()) 
			return false;
		user.setConvalidated(true);
		this.userRepository.save(user);
		return true;
	}
	
	public boolean manageManager(String username, boolean auth) {
		User user = getConvalidatedUser(username);
		if(user == null || user.isManager() == auth) 
			return false;
		user.setManager(auth);
		this.userRepository.save(user);
		return true;
	}
    
    public User getUser(String username) {
    	return this.userRepository.findById(username).orElse(null);
    }
    
    public User getConvalidatedUser(String username) {
    	User user = this.getUser(username);
    	return user != null && user.isConvalidated() ? user : null;
    }
    
    public List<User> getNotConvalidatedUsers(){
    	return StreamSupport.stream(userRepository.findAll().spliterator(), true)
		.filter(u -> !u.isConvalidated()).toList();
    }
    
    private User findCuratorOf(String cityId) {
    	return StreamSupport.stream(userRepository.findAll().spliterator(), true)
				.filter(u -> cityId.equals(u.getCityId())).findFirst().orElse(null);
    }

    public boolean matchCurator(String curator, String cityId) {
    	User user = getConvalidatedUser(curator);
    	if(user == null || user.isCurator())
    		return false;
    	user.setCityId(cityId);
		this.userRepository.save(user);
    	return true;
    }
    
    public boolean changeCurator(String curator, String cityId) {
    	User _old = findCuratorOf(cityId), _new = getConvalidatedUser(curator);
    	if(_old == null || _new == null || _new.isCurator())
    		return false;
    	_old.setCityId(null);
    	_new.setCityId(cityId);
		this.userRepository.save(_old);
		this.userRepository.save(_new);
		return true;
    }
    
    public void discreditCurator(String cityId) {
    	User curator = findCuratorOf(cityId);
    	if(curator != null) {
    		curator.setCityId(null);
    		this.userRepository.save(curator);
    	}

    }

	public boolean usernameExists(String username) {
		return this.userRepository.existsById(username);
	}

	public void send(String username, String message) {
		if(usernameExists(username))
			this.notifications.send(username, message);
	}
    
}
