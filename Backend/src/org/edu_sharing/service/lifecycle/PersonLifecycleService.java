package org.edu_sharing.service.lifecycle;



import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.alfresco.model.ContentModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.OwnableService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.tools.cache.RepositoryCache;
import org.edu_sharing.service.authentication.ScopeUserHomeService;
import org.edu_sharing.service.authentication.ScopeUserHomeServiceFactory;
import org.edu_sharing.service.collection.CollectionService;
import org.edu_sharing.service.collection.CollectionServiceFactory;
import org.springframework.context.ApplicationContext;

import com.google.gdata.data.dublincore.Language;
import com.google.gwt.i18n.client.Constants;


/**
 * Konzept LöschJob -> status todelete
 * 
 * Filter in personsearch (invite, workflow - non active)
 * 
 * validate session only active, 
 * webdav auth, share auth ....
 * 
 * on create person set personstatus to 'active'
 * 
 * job that sets active status for existing persons when edu-sharing property 'person_active_status' is set
 * 
 * user_home remove with/without cc -> with: move cc content to cc_user space (hirachical date folders structure)
 * 
 * 
 * all files in shared content will be deleted except OER(cc) content:
 * student, external, teacher trainee's
 * instance owner becomes owner
 * Collections -> delete?
 * 
 * all files in shared content stay and will be given to an Instanceowner 
 * Teacher, Staff
 * 
 * InviteHistory: delete or rename
 * 
 * filtered in invite dialogs
 * 
 * cause of owner (first/lastname) remove file from properties cache
 * 
 * 
 * @TODO find out instance owner
 * @TODO changing owner to instanceowner, remove old contributer
 * --> no username use firstName and lastName
 * 		Problems: user with same name, and marriage
 * @TODO delete userhome keep CC
 * @TODO instanceowner instead of creator in gui (workspace column)
 * @TODO Collections (only level 0?)
 * @TODO shared content config ROLE_GROUP_REMOVE_SHARED delete cc vs not delete cc
 * @TODO function for changing owner of colletion to another user (asking new user?)
 * @TODO check if Folders must be deleted in shared area, check if basket is necessary
 * @TODO filter for TODELETE_STATUS already in search query
 */
public class PersonLifecycleService {
	
	public static String ROLE_STUDENT = "student";
	public static String ROLE_EXTERNAL = "external";
	public static String ROLE_TEACHER_TRAINEES = "teacher_trainees";
	public String[] ROLE_GROUP_REMOVE_SHARED = {ROLE_STUDENT,ROLE_EXTERNAL,ROLE_TEACHER_TRAINEES};
	
	public static String ROLE_TEACHER = "teacher";
	public static String ROLE_STAFF = "staff";
	public String[] ROLE_GROUP_KEEP_SHARED = {ROLE_TEACHER,ROLE_STAFF};
	
	
	ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
	ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
	
	SearchService searchService = serviceRegistry.getSearchService();
	
	NodeService nodeService = serviceRegistry.getNodeService();
	
	PersonService personService = serviceRegistry.getPersonService();
	
	PermissionService permissionService = serviceRegistry.getPermissionService();
	
	OwnableService ownableService = serviceRegistry.getOwnableService();
	
	int maxItems = 20;
	
	public static String PERSON_STATUS_ACTIVE = "active";
	
	public static String PERSON_STATUS_BLOCKED = "blocked";
	
	public static String PERSON_STATUS_DEACTIVATED = "deactivated";
	
	public static String PERSON_STATUS_TODELETE = "todelete";
	
	boolean keepOERFilesInUserHome = false;
	
	Logger logger = Logger.getLogger(PersonLifecycleService.class);
	
	//public static String ROLE_
	
	public void deletePersons() {
		int skipCount = 0;
		deletePersons(skipCount);		
	}
	
	private void deletePersons(int skipCount) {
		SearchParameters sp = new SearchParameters();
		sp.setQuery("TYPE:\"cm:person\"");
		sp.setSkipCount(skipCount);
		sp.setMaxItems(maxItems);
		ResultSet rs = searchService.query(sp);
		for(NodeRef nodeRef : rs.getNodeRefs()) {
			deletePerson(nodeRef);
		}
		if(rs.hasMore()) {
			deletePersons(skipCount + maxItems);
		}
	}
	
	public void deletePerson(String username) {
		NodeRef personNodeRef = personService.getPerson(username);
		deletePerson(personNodeRef);
	}
	
	public void deletePerson(NodeRef personNodeRef) {
		String status = (String)nodeService.getProperty(personNodeRef, 
				QName.createQName(CCConstants.CM_PROP_PERSON_ESPERSONSTATUS));
		String role = (String)nodeService.getProperty(personNodeRef, QName.createQName(CCConstants.CM_PROP_PERSON_EDU_SCHOOL_PRIMARY_AFFILIATION));
		String userName = (String)nodeService.getProperty(personNodeRef, QName.createQName(CCConstants.CM_PROP_PERSON_USERNAME));
		if(status != null && PERSON_STATUS_TODELETE.equals(status)) {
			
			/**
			 * remove scope safe
			 */
			logger.info("deleting safe shared folders for " + userName);
			deleteSharedContent(personNodeRef, role, CCConstants.CCM_VALUE_SCOPE_SAFE);
			logger.info("deleting safe userhome folders for " + userName);
			deleteScopeUserHome(userName, CCConstants.CCM_VALUE_SCOPE_SAFE, false);
			
			/**
			 * remove default
			 */
			logger.info("deleting collections for " + userName);
			deleteCollections(userName);
			logger.info("deleting shared folders for " + userName);
			deleteSharedContent(personNodeRef, role, null);
			logger.info("deleting userhome folders for " + userName);
			deleteUserHome(personNodeRef, false);
			
			logger.info("deleting person");
			nodeService.addAspect(personNodeRef, ContentModel.ASPECT_TEMPORARY, null);
			personService.deletePerson(personNodeRef,true);
		}
	}
	
	private void deleteSharedContent(NodeRef personNodeRef, String role, String scope) {
		String username = (String)nodeService.getProperty(personNodeRef, 
				QName.createQName(CCConstants.CM_PROP_PERSON_USERNAME));
		NodeRef homeFolder = null;
		if(scope == null) {
			homeFolder = getHomeFolder(personNodeRef);
		}else {
			ScopeUserHomeService scopeUserHomeService = ScopeUserHomeServiceFactory.getScopeUserHomeService();
			homeFolder = scopeUserHomeService.getUserHome(username, scope);
		}
		if(homeFolder != null) {
			List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(homeFolder);
			for(ChildAssociationRef childAssoc : childAssocs) {
				String mapType = (String)nodeService.getProperty(childAssoc.getChildRef(), QName.createQName(CCConstants.CCM_PROP_MAP_TYPE));
				if(CCConstants.CCM_VALUE_MAP_TYPE_EDUGROUP.equals(mapType)){
					/**
					 * @TODO find out instance owner
					 */
					deleteSharedContent(childAssoc.getChildRef(), role, username,"admin");
				}
			}
		}
	}
	
	
	private void deleteSharedContent(NodeRef parent, String role, String user, String instanceOwner){
		List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(parent);
		for(ChildAssociationRef childAssoc : childAssocs) {
			NodeRef nodeRef = childAssoc.getChildRef();
			
			QName nodeType = nodeService.getType(childAssoc.getChildRef());
			
			if(nodeType.equals(QName.createQName(CCConstants.CCM_TYPE_IO))){
				String owner = (String)ownableService.getOwner(nodeRef);
				if(owner.equals(user)) {
					ArrayList<String> licenseKeys = (ArrayList<String>)nodeService.getProperty(nodeRef, 
							QName.createQName(CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY));
					
					
					boolean isCCLicense = false;
					if(licenseKeys != null) {
						for(String license : licenseKeys) {
							if(license.startsWith("CC_")) {
								isCCLicense = true;
							}
						}
					}
					
					if(!isCCLicense) {
						if(Arrays.asList(ROLE_GROUP_REMOVE_SHARED).contains(role)) {
							/**
							 * remove without archiving
							 */
							nodeService.addAspect(nodeRef, ContentModel.ASPECT_TEMPORARY, null);
							nodeService.deleteNode(nodeRef);
						}if(Arrays.asList(ROLE_GROUP_KEEP_SHARED).contains(role)) {
							ownableService.setOwner(nodeRef, instanceOwner);
							new RepositoryCache().remove(nodeRef.getId());
						}
					}else {
						ownableService.setOwner(nodeRef, instanceOwner);
						new RepositoryCache().remove(nodeRef.getId());
					}
				}
			}
			if(nodeType.equals(QName.createQName(CCConstants.CCM_TYPE_MAP))){
				deleteSharedContent(nodeRef, role, user, instanceOwner);
			}
		}
	}
	
	public void deleteCollections(String userName) {

		SearchParameters sp = new SearchParameters();
		//sp.setQuery("ASPECT:\"ccm:collection\" AND @ccm\\:collectionlevel0:true AND OWNER:\""+userName+"\"");
		sp.setQuery("ASPECT:\"ccm:collection\" AND OWNER:\""+userName+"\"");
		sp.setSkipCount(0);
		sp.setMaxItems(-1);
		sp.addStore(MCAlfrescoAPIClient.storeRef);
		sp.setLanguage(SearchService.LANGUAGE_LUCENE);
		ResultSet rs = searchService.query(sp);
		for(NodeRef nodeRef : rs.getNodeRefs()) {
			String collection = (String)nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CM_NAME));
			logger.info("deleteing collection:" + collection);
			nodeService.addAspect(nodeRef, ContentModel.ASPECT_TEMPORARY, null);
			nodeService.deleteNode(nodeRef);
		}
	}
	
	private void deleteScopeUserHome(String username, String scope, boolean keepCC) {
		ScopeUserHomeService scopeUserHomeService = ScopeUserHomeServiceFactory.getScopeUserHomeService();
		NodeRef homeFolder = scopeUserHomeService.getUserHome(username, scope);
		if(homeFolder == null) {
			return;
		}
		if(keepCC) {
			//@TODO
			logger.info("not implemented yet");
			return;
		}else {
			/**
			 * remove without archiving
			 */
			nodeService.addAspect(homeFolder, ContentModel.ASPECT_TEMPORARY, null);
			nodeService.deleteNode(homeFolder);
		}
	}
	
	
	
	private boolean keepSharedContent(String role){
		return false;
	}
	private void deleteUserHome(NodeRef personNodeRef, boolean keepCC) {
		NodeRef homeFolder = getHomeFolder(personNodeRef);
		
		if(keepCC) {
			//@TODO
			logger.info("not implemented yet");
			return;
		}else {
			/**
			 * remove without archiving
			 */
			nodeService.addAspect(homeFolder, ContentModel.ASPECT_TEMPORARY, null);
			nodeService.deleteNode(homeFolder);
		}
		
	}
	
	private NodeRef getHomeFolder(NodeRef personNodeRef) {
		NodeRef homeFolder = (NodeRef)nodeService.getProperty(personNodeRef, 
				QName.createQName(CCConstants.CM_PROP_PERSON_HOME_FOLDER));
		return homeFolder;
	}

}
