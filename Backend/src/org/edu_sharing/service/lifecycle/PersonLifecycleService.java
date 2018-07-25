package org.edu_sharing.service.lifecycle;



import java.security.acl.Owner;
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
import org.springframework.context.ApplicationContext;

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
 * 
 * 
 */
public class PersonLifecycleService {
	
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
			String status = (String)nodeService.getProperty(nodeRef, 
					QName.createQName(CCConstants.CM_PROP_PERSON_ESPERSONSTATUS));
			String role = (String)nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CM_PROP_PERSON_EDU_SCHOOL_PRIMARY_AFFILIATION));
			if(status != null && PERSON_STATUS_TODELETE.equals(status)) {
				
				
				deleteSharedContent(nodeRef, role);
				deleteUserHome(nodeRef,false);
				personService.deletePerson(nodeRef,true);
			}
		}
		if(rs.hasMore()) {
			deletePersons(skipCount + maxItems);
		}
	}
	
	private void deleteSharedContent(NodeRef personNodeRef, String role) {
		String username = (String)nodeService.getProperty(personNodeRef, 
				QName.createQName(CCConstants.CM_PROP_PERSON_USERNAME));
		NodeRef homeFolder = getHomeFolder(personNodeRef);
		List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(homeFolder);
		for(ChildAssociationRef childAssoc : childAssocs) {
			String mapType = (String)nodeService.getProperty(childAssoc.getChildRef(), QName.createQName(CCConstants.CCM_PROP_MAP_TYPE));
			if(CCConstants.CCM_VALUE_MAP_TYPE_EDUGROUP.equals(mapType)){
				deleteSharedContent(childAssoc.getChildRef(), role, username);
			}
		}
	}
	
	private void deleteSharedContent(NodeRef parent, String role, String user){
		List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(parent);
		for(ChildAssociationRef childAssoc : childAssocs) {
			QName nodeType = nodeService.getType(childAssoc.getChildRef());
			if(nodeType.equals(QName.createQName(CCConstants.CCM_TYPE_IO))){
				
				String owner = (String)ownableService.getOwner(childAssoc.getChildRef());
				if(owner.equals(user)) {
					
				}
			}
			if(nodeType.equals(QName.createQName(CCConstants.CCM_TYPE_MAP))){
				
			}
		}
	}
	
	private boolean keepSharedContent(String role){
		
	}
	private void deleteUserHome(NodeRef personNodeRef, boolean keepCC) {
		NodeRef homeFolder = getHomeFolder(personNodeRef);
		
		if(keepCC) {
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
