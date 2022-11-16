package org.edu_sharing.restservices;

import java.util.*;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.PersonService;
import org.edu_sharing.metadataset.v2.*;
import org.edu_sharing.metadataset.v2.tools.MetadataHelper;
import org.edu_sharing.metadataset.v2.tools.MetadataSearchHelper;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.RepoFactory;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.mailtemplates.MailTemplate;
import org.edu_sharing.restservices.mds.v1.model.*;
import org.edu_sharing.restservices.shared.MdsQueryCriteria;
import org.edu_sharing.restservices.shared.Mds;
import org.edu_sharing.restservices.shared.Node;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.service.authority.AuthorityServiceHelper;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.permission.PermissionServiceFactory;
import org.edu_sharing.service.search.Suggestion;
import org.springframework.security.core.Authentication;

public class MdsDao {

	public static final String DEFAULT = "-default-";

	public static List<MetadataSetInfo> getAllMdsDesc(RepositoryDao repoDao) throws Exception {
		return RepoFactory.getMetadataSetsForRepository(repoDao.getId());
	}
	
	public Suggestions getSuggestions(String queryId,String parameter,String value, List<MdsQueryCriteria> criterias) throws DAOException{
		Suggestions result = new Suggestions();
		ArrayList<Suggestions.Suggestion> suggestionsResult = new ArrayList<>();
		result.setValues(suggestionsResult);
		try{
			List<? extends Suggestion>  suggestions =
					MetadataSearchHelper.getSuggestions(this.repoDao.getId(), mds, queryId, parameter, value, criterias);

			for(Suggestion suggest : suggestions){
				Suggestions.Suggestion suggestion = new Suggestions.Suggestion();
				suggestion.setDisplayString(suggest.getDisplayString());
				//suggestion.setReplacementString(suggest.getReplacementString());
				suggestion.setKey(suggest.getKey());
				suggestionsResult.add(suggestion);
			}
			
		}catch(Exception e){
			throw DAOException.mapping(e);
		}
		return result;
	}
	
	public static MdsDao getMds(RepositoryDao repoDao, String mdsId) throws DAOException {

		try {
			
			MetadataSet mds=MetadataHelper.getMetadataset(repoDao.getApplicationInfo(),mdsId);
			
			if (mds == null) {
				throw new DAOMissingException(new IllegalArgumentException(mdsId));
			}
			
			return new MdsDao(repoDao, mds);
			
		} catch (Throwable t) {
			throw DAOException.mapping(t);
		}
		
	}

	private final RepositoryDao repoDao;	
	private final MetadataSet mds;
	
	private MdsDao(RepositoryDao repoDao, MetadataSet mds) {
		this.repoDao = repoDao;		
		this.mds = mds;		
	}

	public Mds asMds() {
		
    	Mds data = new Mds();
    	
    	data.setName(mds.getName());
    	data.setCreate(mds.getCreate()!=null ? new Mds.Create(mds.getCreate()) : null);
    	data.setWidgets(getWidgets());
    	data.setViews(getViews());
    	data.setGroups(getGroups());
    	data.setLists(getLists());
    	data.setSorts(getSorts());

    	return data; 
	}

	private List<MdsWidget> getWidgets() {
		List<MdsWidget> result = new ArrayList<MdsWidget>();
		for (MetadataWidget type : this.mds.getWidgets()) {
			result.add(new MdsWidget(type));
		}
		return result;		
	}
	
	private List<MdsView> getViews() {
		List<MdsView> result = new ArrayList<MdsView>();
		for (MetadataTemplate type : this.mds.getTemplates()) {
			result.add(new MdsView(type));
		}
		return result;		
	}
	
	private List<MdsGroup> getGroups() {
		List<MdsGroup> result = new ArrayList<MdsGroup>();
		for (MetadataGroup type : this.mds.getGroups()) {
			result.add(new MdsGroup(type));
		}
		return result;		
	}
	
	private List<MdsList> getLists() {
		List<MdsList> result = new ArrayList<>();
		for (MetadataList type : this.mds.getLists()) {
			result.add(new MdsList(type));
		}
		return result;		
	}

	private List<MdsSort> getSorts() {
		List<MdsSort> result = new ArrayList<>();
		for (MetadataSort type : this.mds.getSorts()) {
			result.add(new MdsSort(type));
		}
		return result;
	}
	
	public MetadataSet getMds() {
		return mds;
	}

	public MdsValue suggestValue(String widget, String valueCaption, String parent, List<String> nodes) throws DAOException {
		Optional<MetadataWidget> widgetDefinition = mds.findAllWidgets(widget).stream().filter((w) -> w.getSuggestionReceiver() != null && !w.getSuggestionReceiver().isEmpty()).findFirst();
		if (!widgetDefinition.isPresent()) {
			throw new DAOValidationException(new IllegalArgumentException("No widget definition found which can receive suggestion data"));
		}
		MdsValue result = new MdsValue();
		result.setId(UUID.randomUUID().toString());
		result.setParent(parent);
		result.setCaption(valueCaption);
		String currentUser = AuthenticationUtil.getFullyAuthenticatedUser();
		Map<String, String> replace = new HashMap<>();
		if(currentUser != null) {
			NodeRef userRef = AuthorityServiceHelper.getAuthorityNodeRef(currentUser);
			if (userRef != null) {
				replace.put("firstName", NodeServiceHelper.getProperty(userRef, CCConstants.CM_PROP_PERSON_FIRSTNAME));
				replace.put("lastName", NodeServiceHelper.getProperty(userRef, CCConstants.CM_PROP_PERSON_LASTNAME));
			}
		}
		replace.put("widgetId", widgetDefinition.get().getId());
		replace.put("widgetCaption", widgetDefinition.get().getCaption());
		replace.put("caption", valueCaption);
		replace.put("id", result.getId());
		replace.put("parentId", parent);
		replace.put("parentCaption", parent == null ? null : widgetDefinition.get().getValuesAsMap().get(parent).getCaption());
		if(nodes != null && !nodes.isEmpty()) {
			try {
				MailTemplate.applyNodePropertiesToMap("node.", NodeServiceHelper.getProperties(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodes.get(0))), replace);
				replace.put("link", MailTemplate.generateContentLink(ApplicationInfoList.getHomeRepository(), nodes.get(0)));
			} catch (Throwable t) {
				throw DAOException.mapping(t);
			}
		}
		String[] receiver = widgetDefinition.get().getSuggestionReceiver().split(",");
		Arrays.stream(receiver).forEach((r) -> {
			try {
				MailTemplate.sendMail(r, "mdsValuespaceSuggestion", replace);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
		return result;
	}
}
