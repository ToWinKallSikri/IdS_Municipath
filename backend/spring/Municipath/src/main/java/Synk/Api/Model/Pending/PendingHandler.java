package Synk.Api.Model.Pending;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.StreamSupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import Synk.Api.Model.IdentifierManager;
import Synk.Api.Model.MuniciPathMediator;
import Synk.Api.Model.Post.PostType;

@Repository
public class PendingHandler {

	private MuniciPathMediator mediator;

	@Autowired
	private PendingRepository pendingRepository;
	private IdentifierManager idManager = new IdentifierManager();

	public void setMediator(MuniciPathMediator mediator) {
        this.mediator = mediator;
    }
	
	public void addRequest(String id) {
		this.pendingRepository.save(new PendingRequest(id));
	}
	
	public void addPostRequest(String postId, String title, PostType type, String text,
			List<String> data, LocalDateTime start, LocalDateTime end, boolean persistence) {
		this.pendingRepository.save(new PendingRequest(postId, title, text, persistence, type, data, start, end));
	}

	public void addGroupRequest(String groupId, String title, boolean sorted, List<String> postIds, 
			LocalDateTime start, LocalDateTime end, boolean persistence) {
		this.pendingRepository.save(new PendingRequest(groupId, title, sorted, persistence, postIds, start, end));
	}
	
	public boolean judge(String pendingId, boolean outcome, String motivation) {
		PendingRequest request = getRequest(pendingId);
		if(request == null)
			return false;
		boolean isGroup = idManager.isGroup(pendingId);
		String username = this.mediator.getAuthor(pendingId);
		String response = "Contenuto " + (outcome ? "accettato" : "rifiutato") + ".\n";
		if(outcome) {
			if(isGroup) 
				mediator.manageGroupRequest(request);
			else mediator.managePostRequest(request);
		} else if(request.isNew()) {
			if(isGroup) 
				this.mediator.deletePendingGroup(pendingId);
			else this.mediator.deletePendingPost(pendingId);
		}
		this.mediator.send(username, response+outcome);
		this.pendingRepository.delete(request);
		return true;
	}


	public List<PendingRequest> getAllRequest(String cityId){
		return StreamSupport.stream(pendingRepository.findAll().spliterator(), true)
				.filter(p -> idManager.getCityId(p.getId()).equals(cityId)).toList();
	}
	
	public PendingRequest getRequest(String requestId) {
		return this.pendingRepository.findById(requestId).orElse(null);
	}


	
}
