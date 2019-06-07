package org.edu_sharing.service.authority;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import javax.transaction.UserTransaction;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.security.permissions.AccessDeniedException;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.OwnableService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.workspace_administration.NodeServiceInterceptor;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.rpc.EduGroup;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.PropertyRequiredException;
import org.edu_sharing.repository.server.authentication.Context;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.restservices.DAOException;
import org.edu_sharing.service.Constants;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.NotAnAdminException;
import org.springframework.context.ApplicationContext;

public class AuthorityServiceImpl implements AuthorityService {

	Logger logger = Logger.getLogger(AuthorityServiceImpl.class);

	ApplicationContext alfApplicationContext = AlfAppContextGate.getApplicationContext();
	ServiceRegistry serviceRegistry = (ServiceRegistry) alfApplicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
	org.alfresco.service.cmr.security.AuthorityService authorityService = serviceRegistry.getAuthorityService();
	NodeService nodeService = serviceRegistry.getNodeService();
	OwnableService ownableService = serviceRegistry.getOwnableService();
	PermissionService permissionService = serviceRegistry.getPermissionService();
	MCAlfrescoAPIClient baseClient = new MCAlfrescoAPIClient();


	/**
	 * Returns a property for a certain authority (it will fetch the coressponding node and load the property)
	 * @param authority
	 * @param property
	 * @return
	 */
	@Override
	public Object getAuthorityProperty(String authority,String property){
		return nodeService.getProperty(authorityService.getAuthorityNodeRef(authority),QName.createQName(property));
	}
	/**
	 * returns the node id for the given authority (useful if you want to change metadata)
	 * @param authority
	 * @return
	 */
	@Override
	public NodeRef getAuthorityNodeRef(String authority){
		return authorityService.getAuthorityNodeRef(authority);
	}
	@Override
	public void setAuthorityProperty(String authority,String property,Serializable value){
		nodeService.setProperty(authorityService.getAuthorityNodeRef(authority),QName.createQName(property),value);
	}
	@Override
	public void addAuthorityAspect(String authority,String aspect){
		nodeService.addAspect(authorityService.getAuthorityNodeRef(authority),QName.createQName(aspect),new HashMap<>());
	}
	@Override
	public void deleteAuthority(String authorityName) {
				
		serviceRegistry.getTransactionService().getRetryingTransactionHelper().doInTransaction(
				
                new RetryingTransactionCallback<Void>()
                {
                    public Void execute() throws Throwable
                    {
                		String key =  authorityName;
                		String groupType = (String) getAuthorityProperty(key,CCConstants.CCM_PROP_GROUPEXTENSION_GROUPTYPE);
                		if(groupType!=null && groupType.equals(ADMINISTRATORS_GROUP_TYPE) && !baseClient.isAdmin(AuthenticationUtil.getFullyAuthenticatedUser()))
                			throw new AccessDeniedException("An admin group can not be deleted");
                		authorityService.deleteAuthority(key, true);

                		return null;
                    }
                }, false); 
	}

	@Override
	public boolean hasModifyAccessToGroup(String groupName){
    	Set<String> memberships=serviceRegistry.getAuthorityService().getAuthorities();
    	if(memberships.contains(CCConstants.AUTHORITY_GROUP_ALFRESCO_ADMINISTRATORS))
			return true;
    	// Detect the group prefix and decide
    	String[] split=groupName.split("_");
    	if(split.length<2)
    		return false;
    	String adminGroup=PermissionService.GROUP_PREFIX+split[0]+"_"+ADMINISTRATORS_GROUP;
    	if(memberships.contains(adminGroup))
    		return groupIsOfType(adminGroup,ADMINISTRATORS_GROUP_TYPE);
    	return false;
	}
	private boolean groupIsOfType(String adminGroup, String type) {
		String typeProp=(String) serviceRegistry.getNodeService().getProperty(serviceRegistry.getAuthorityService().getAuthorityNodeRef(adminGroup),QName.createQName(CCConstants.CCM_PROP_GROUPEXTENSION_GROUPTYPE));
    	return typeProp.equals(type);
	}
	@Override
	public boolean isGuest() {
		try {
			ApplicationInfo appInfo = ApplicationInfoList.getHomeRepository();
			String guest = appInfo.getGuest_username();
			if(guest != null && guest.equals(AuthenticationUtil.getFullyAuthenticatedUser())){
				return true;
			}
		} catch (Throwable e) {
		}
		return false;
	}
	@Override
	public boolean hasAdminAccessToGroup(String groupName){
		try {
	    	Set<String> memberships=serviceRegistry.getAuthorityService().getAuthorities();
			if(memberships.contains(CCConstants.AUTHORITY_GROUP_ALFRESCO_ADMINISTRATORS))
				return true;
			String group=PermissionService.GROUP_PREFIX+AuthorityService.getGroupName(ADMINISTRATORS_GROUP,groupName);
			if(memberships.contains(group))
				return groupIsOfType(group, ADMINISTRATORS_GROUP_TYPE);
			
			// Detect the group prefix and decide
			String[] split=groupName.split("_");
			if(split.length<2)
				return false;
			group=PermissionService.GROUP_PREFIX+split[0]+"_"+ADMINISTRATORS_GROUP;
			if(memberships.contains(group))
				return groupIsOfType(group, ADMINISTRATORS_GROUP_TYPE);
			
		} catch (Throwable t) {
			t.printStackTrace();
		}
		return false;
	}
	public String getProperty(String authorityName, String property){
		return (String)nodeService.getProperty(authorityService.getAuthorityNodeRef(authorityName),QName.createQName(property));
	}
	@Override
	public boolean hasAdminAccessToOrganization(String orgName){
		try {
	    	Set<String> memberships=serviceRegistry.getAuthorityService().getAuthorities();
			if(memberships.contains(CCConstants.AUTHORITY_GROUP_ALFRESCO_ADMINISTRATORS))
				return true;
			
			
			String group=PermissionService.GROUP_PREFIX+AuthorityService.getGroupName(ADMINISTRATORS_GROUP,orgName);
			if(memberships.contains(group))
				return groupIsOfType(group,ADMINISTRATORS_GROUP_TYPE);
			
		} catch (Throwable t) {
			t.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean isGlobalAdmin() {
		try{
			String user = AuthenticationUtil.getFullyAuthenticatedUser();
			if("admin".equals(user)){
				return true;
			}
			if(AuthenticationUtil.isRunAsUserTheSystemUser()){
				return true;
			}
			return getMemberships(user).contains(CCConstants.AUTHORITY_GROUP_ALFRESCO_ADMINISTRATORS);
		}catch(Throwable t){
			return false;
		}
	}

	@Override
	public ArrayList<EduGroup> getAllEduGroups() {
		org.alfresco.service.cmr.security.AuthorityService alfAuthorityService = serviceRegistry.getAuthorityService();
		Set<String> authoritiesForUser = alfAuthorityService
				.getAuthoritiesForUser(AuthenticationUtil.getFullyAuthenticatedUser());

		
		
		ArrayList<EduGroup> result = new ArrayList<EduGroup>();

		for (String authority : authoritiesForUser) {
			EduGroup eg = getEduGroup(authority);
			if(eg != null)result.add(eg);
		}

		return result;
	}
	
@Override	
public EduGroup getEduGroup(String authority){
		NodeRef nodeRef = authorityService.getAuthorityNodeRef(authority);
		if(nodeRef == null){
			return null;
		}
		
		NodeRef nodeRefEduGroupHomeDir = (NodeRef) nodeService.getProperty(nodeRef,
				QName.createQName(CCConstants.CCM_PROP_EDUGROUP_EDU_HOMEDIR));
		if (nodeRefEduGroupHomeDir != null) {
	
			Map<QName, Serializable> folderProps = nodeService.getProperties(nodeRefEduGroupHomeDir);
			EduGroup eduGroup = new EduGroup();
			eduGroup.setFolderId((String) folderProps.get(QName.createQName(CCConstants.SYS_PROP_NODE_UID)));
			eduGroup.setFolderName((String) folderProps.get(QName.createQName(CCConstants.CM_NAME)));
	
			Map<QName, Serializable> groupProps = nodeService.getProperties(nodeRef);
	
			eduGroup.setGroupId((String) groupProps.get(QName.createQName(CCConstants.SYS_PROP_NODE_UID)));
			eduGroup.setGroupname(
					(String) groupProps.get(QName.createQName(CCConstants.CM_PROP_AUTHORITY_AUTHORITYNAME)));
			eduGroup.setGroupDisplayName(
					(String) groupProps.get(QName.createQName(CCConstants.CM_PROP_AUTHORITY_AUTHORITYDISPLAYNAME)));
			eduGroup.setFolderPath(nodeService.getPath(nodeRefEduGroupHomeDir)
					.toPrefixString(serviceRegistry.getNamespaceService()));
			eduGroup.setScope((String) groupProps.get(QName.createQName(CCConstants.CCM_PROP_EDUSCOPE_NAME)));
			
			return eduGroup;
		}
		
		return null;
	}

	@Override
	public ArrayList<EduGroup> getEduGroups() {
		String currentScope = NodeServiceInterceptor.getEduSharingScope();
		return getEduGroups(currentScope);
	}

	@Override
	public ArrayList<EduGroup> getEduGroups(String scope) {
		ArrayList<EduGroup> result = new ArrayList<EduGroup>();

		for (EduGroup eduGroup : getAllEduGroups()) {
			if ((eduGroup.getScope() == null && scope == null)
					|| (eduGroup.getScope() != null && eduGroup.getScope().equals(scope))) {
				result.add(eduGroup);
			}
		}
		return result;
	}
	@Override
	public Set<String> getMemberships(String username) {
		return serviceRegistry.getAuthorityService().getAuthoritiesForUser(username);
	}
	@Override
	public String createGroup(String groupName, String displayName,String parentGroup) throws DAOException {	
		try {
			
			if(parentGroup!=null && parentGroup.isEmpty())
				parentGroup=null;
			
			if(parentGroup==null){
				if(!isGlobalAdmin())
					throw new AccessDeniedException("No permission to create global group");
			}
			else if(!hasAdminAccessToGroup(parentGroup)){
				throw new AccessDeniedException("No permission to create group in "+parentGroup);
			}
			final String parentGroupFinal=parentGroup;
			return AuthenticationUtil.runAsSystem(new RunAsWork<String>() {

				@Override
				public String doWork() throws Exception {
					return baseClient.createOrUpdateGroup(groupName, displayName,parentGroupFinal,true);
				}
			});
			
		} catch (Exception e) {

			throw DAOException.mapping(e);
		}
	}
	public EduGroup getOrCreateEduGroup(EduGroup eduGroup, EduGroup unscopedEduGroup, String folderParentId) {

		AuthenticationUtil.RunAsWork<EduGroup> createEduGroupWorker = new AuthenticationUtil.RunAsWork<EduGroup>() {
			@Override
			public EduGroup doWork() throws Exception {
				ReentrantLock lock = new ReentrantLock();
				lock.lock();
				UserTransaction transaction = serviceRegistry.getTransactionService()
						.getNonPropagatingUserTransaction(false);

			
				if (!authorityService.getAllAuthorities(AuthorityType.GROUP).contains(eduGroup.getGroupname())) {
					try {
						transaction.begin();

						String authority = authorityService.createAuthority(AuthorityType.GROUP,
								eduGroup.getGroupname().replaceAll("GROUP_", ""));
						NodeRef nodeRef = authorityService.getAuthorityNodeRef(authority);
						nodeService.setProperty(nodeRef,
								QName.createQName(CCConstants.CM_PROP_AUTHORITY_AUTHORITYDISPLAYNAME),
								eduGroup.getGroupDisplayName());

						// scope
						Map<QName, Serializable> propsAspectEduScope = new HashMap<QName, Serializable>();
						propsAspectEduScope.put(QName.createQName(CCConstants.CCM_PROP_EDUSCOPE_NAME),
								eduGroup.getScope());
						nodeService.addAspect(nodeRef, QName.createQName(CCConstants.CCM_ASPECT_EDUSCOPE),
								propsAspectEduScope);

						String eduGroupHomeFolderId = eduGroup.getFolderId();
						if (eduGroupHomeFolderId == null) {
							String folderName = eduGroup.getFolderName();
							if (folderName == null) {
								folderName = eduGroup.getGroupname().replace(PermissionService.GROUP_PREFIX,"");
								if (eduGroup.getScope() != null) {
									folderName = folderName + "_" + eduGroup.getScope();
								}
							}
							Map<QName, Serializable> folderProps = new HashMap<QName, Serializable>();
							folderProps.put(ContentModel.PROP_NAME, folderName);

							String assocName = "{" + CCConstants.NAMESPACE_CCM + "}" + folderName;
							ChildAssociationRef newNode = nodeService.createNode(new NodeRef(Constants.storeRef, folderParentId),
									ContentModel.ASSOC_CONTAINS, QName.createQName(assocName),
									QName.createQName(CCConstants.CCM_TYPE_MAP), folderProps);
							
							nodeService.addAspect(newNode.getChildRef(), QName.createQName(CCConstants.CCM_ASPECT_EDUSCOPE),
									propsAspectEduScope);
							ownableService.setOwner(newNode.getChildRef(), AuthenticationUtil.getRunAsUser());
							serviceRegistry.getPermissionService().setPermission(newNode.getChildRef(), eduGroup.getGroupname(), PermissionService.READ, true);
							serviceRegistry.getPermissionService().setInheritParentPermissions(newNode.getChildRef(), false);
							eduGroupHomeFolderId = newNode.getChildRef().getId();
						}

						// edugroup aspect
						Map<QName, Serializable> propsAspectEduGroup = new HashMap<QName, Serializable>();
						propsAspectEduGroup.put(QName.createQName(CCConstants.CCM_PROP_EDUGROUP_EDU_HOMEDIR),
								new NodeRef(Constants.storeRef, eduGroupHomeFolderId));
						nodeService.addAspect(nodeRef, QName.createQName(CCConstants.CCM_ASPECT_EDUGROUP),
								propsAspectEduGroup);
						
						
						//copy ORG_ADMINISTRATORS
						if(unscopedEduGroup != null && eduGroup.getScope() != null){
							Set<String> containedAuthorities = authorityService.getContainedAuthorities(AuthorityType.GROUP, unscopedEduGroup.getGroupname(), true);
							for(String containedAuthority : containedAuthorities){
								NodeRef containedAuthorityNodeRef = authorityService.getAuthorityNodeRef(containedAuthority);
								String groupType = (String)nodeService.getProperty(containedAuthorityNodeRef,QName.createQName(CCConstants.CCM_PROP_GROUPEXTENSION_GROUPTYPE));
								if(ADMINISTRATORS_GROUP_TYPE.equals(groupType)){
									String groupname = (String)nodeService.getProperty(containedAuthorityNodeRef, QName.createQName(CCConstants.CM_PROP_AUTHORITY_AUTHORITYNAME));
									groupname = groupname.replace(AuthorityType.GROUP.getPrefixString(), "");
									String groupDisplayName = (String)nodeService.getProperty(containedAuthorityNodeRef, QName.createQName(CCConstants.CM_PROP_AUTHORITY_AUTHORITYDISPLAYNAME));
									String groupAdministrators = authorityService.createAuthority(AuthorityType.GROUP, groupname + "_" + eduGroup.getScope() , groupDisplayName + "_"  + eduGroup.getScope(), authorityService.getDefaultZones());
									NodeRef groupAdministratorsNodeRef = authorityService.getAuthorityNodeRef(groupAdministrators);
									nodeService.setProperty(groupAdministratorsNodeRef, QName.createQName(CCConstants.CCM_PROP_GROUPEXTENSION_GROUPTYPE), ADMINISTRATORS_GROUP_TYPE);
									//scope
									nodeService.addAspect(groupAdministratorsNodeRef, QName.createQName(CCConstants.CCM_ASPECT_EDUSCOPE), propsAspectEduScope);
									
									authorityService.addAuthority(eduGroup.getGroupname(), groupAdministrators);
									permissionService.setPermission(new NodeRef(Constants.storeRef,eduGroupHomeFolderId), groupAdministrators, CCConstants.PERMISSION_ES_CHILD_MANAGER, true);
								}
							}
						}

						

					} catch (Throwable e) {
						logger.error(e.getMessage(), e);
						try {
							transaction.rollback();
						} catch (Exception e2) {
							logger.error(e.getMessage(), e2);
						}
					}
					transaction.commit();
				}
				
				lock.unlock();
				
				return getEduGroup(eduGroup.getGroupname());
			}
		};
		
		return AuthenticationUtil.runAs(createEduGroupWorker, ApplicationInfoList.getHomeRepository().getUsername());

	}
	
	@Override
	public boolean authorityExists(String authority) {
		return authorityService.authorityExists(authority);
	}
	/**
	 * returns null when user not exists
	 */
	@Override
	public Map<String, Serializable> getUserInfo(String userName) throws Exception {

		return serviceRegistry.getTransactionService().getRetryingTransactionHelper().doInTransaction(

                new RetryingTransactionCallback<Map<String, Serializable>>()
                {
                    public Map<String, Serializable> execute() throws Throwable
                    {
                		NodeRef personRef = serviceRegistry.getPersonService().getPerson(userName, false);
                		if (personRef == null) {
                			return null;
                		}

                		Map<QName, Serializable> tmpProps = nodeService.getProperties(personRef);
                		HashMap<String, Serializable> result = new HashMap<String, Serializable>();
                		for (Map.Entry<QName, Serializable> entry : tmpProps.entrySet()) {

                			Serializable value = entry.getValue();

                			result.put(
                					entry.getKey().toString(),
                					value);
                		}
                		return result;
                    }
                }, true);



		}
		@Override
		public void createOrUpdateUser(Map<String, Serializable> userInfo) throws Exception {

			String currentUser = AuthenticationUtil.getRunAsUser();

			if(userInfo == null){
				throw new PropertyRequiredException(CCConstants.CM_PROP_PERSON_USERNAME);
			}

			String userName = (String) userInfo.get(CCConstants.CM_PROP_PERSON_USERNAME);
			String firstName = (String) userInfo.get(CCConstants.CM_PROP_PERSON_FIRSTNAME);
			String lastName = (String) userInfo.get(CCConstants.CM_PROP_PERSON_LASTNAME);
			String email = (String) userInfo.get(CCConstants.CM_PROP_PERSON_EMAIL);

			if(userName == null || userName.trim().equals("")){
				throw new PropertyRequiredException(CCConstants.CM_PROP_PERSON_USERNAME);
			}

			if(firstName == null || firstName.trim().equals("")){
				throw new PropertyRequiredException(CCConstants.CM_PROP_PERSON_FIRSTNAME);
			}

			if(lastName == null || lastName.trim().equals("")){
				throw new PropertyRequiredException(CCConstants.CM_PROP_PERSON_LASTNAME);
			}

			if(email == null || email.trim().equals("")){
				throw new PropertyRequiredException(CCConstants.CM_PROP_PERSON_EMAIL);
			}

			if (!currentUser.equals(userName) && !AuthorityServiceFactory.getLocalService().isGlobalAdmin()) {
				throw new NotAnAdminException();
			}

			PersonService personService = serviceRegistry.getPersonService();

			serviceRegistry.getTransactionService().getRetryingTransactionHelper().doInTransaction(

		        new RetryingTransactionCallback<Void>()
		        {
		            public Void execute() throws Throwable
		            {
		        		Throwable runAs = AuthenticationUtil.runAs(

		    				new AuthenticationUtil.RunAsWork<Throwable>() {

		    					@Override
		    					public Throwable doWork() throws Exception {

		    						try {

		    	                    	if (personService.personExists(userName)) {

		    	                			personService.setPersonProperties(userName, transformQName(userInfo));

		    	                		} else {

		    	                			personService.createPerson(transformQName(userInfo));
		    	                		}
		    	                    	addUserExtensionAspect(userName);

		    						} catch (Throwable e) {
		    							logger.error(e.getMessage(), e);
		    							return e;
		    						}

		    						return null;
		    					}

		    				},
		    				ApplicationInfoList.getHomeRepository().getUsername());

		        		if (runAs != null) {
		        			throw runAs;
		        		}

		        		return null;
		            }

		        },
		        false);
		}
		private void addUserExtensionAspect(String userName) {
			PersonService personService = serviceRegistry.getPersonService();
			if(!nodeService.hasAspect(personService.getPerson(userName),QName.createQName(CCConstants.CCM_ASPECT_USER_EXTENSION)))
					nodeService.addAspect(personService.getPerson(userName),QName.createQName(CCConstants.CCM_ASPECT_USER_EXTENSION),null);
		}
		private Map<QName, Serializable> transformQName(Map<String, Serializable> data) {
			Map<QName, Serializable> transformed = new HashMap<QName, Serializable>();
			for(String key : data.keySet()) {
				transformed.put(QName.createQName(key), data.get(key));
			}
			return transformed;
		}
}
