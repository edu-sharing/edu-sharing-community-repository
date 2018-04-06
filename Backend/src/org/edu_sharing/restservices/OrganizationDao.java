package org.edu_sharing.restservices;

import java.util.List;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.security.permissions.AccessDeniedException;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.security.PermissionService;
import org.edu_sharing.repository.client.rpc.ACE;
import org.edu_sharing.repository.client.rpc.EduGroup;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.restservices.shared.Authority;
import org.edu_sharing.restservices.shared.GroupProfile;
import org.edu_sharing.restservices.shared.NodeRef;
import org.edu_sharing.restservices.shared.Organization;
import org.edu_sharing.service.authority.AuthorityService;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.organization.OrganizationService;
import org.edu_sharing.service.organization.OrganizationServiceFactory;
import org.edu_sharing.service.permission.PermissionServiceFactory;
import org.edu_sharing.service.search.SearchServiceFactory;

import com.sun.star.lang.IllegalArgumentException;

public class OrganizationDao {

	public static List<EduGroup> getOrganizations(RepositoryDao repoDao) throws DAOException {
		try{
			return SearchServiceFactory.getSearchService(repoDao.getApplicationInfo().getAppId()).searchOrganizations("", 0, Integer.MAX_VALUE, null,false).getData();
		}catch(Throwable t){
			throw DAOException.mapping(t);
		}
	}

	public static String create(RepositoryDao repoDao, String groupName, String folderId) throws DAOException {

		try {
			
			String currentUser = AuthenticationUtil.getFullyAuthenticatedUser(); 
			
			if (!repoDao.getBaseClient().isAdmin(currentUser)) {
								
				throw new AccessDeniedException(currentUser);
			}
				
			((MCAlfrescoAPIClient)repoDao.getBaseClient()).bindEduGroupFolder(groupName, folderId);
			return groupName;
		} catch (Exception e) {

			throw DAOException.mapping(e);
		}
				
	}
	
	public static GroupDao create(RepositoryDao repoDao, String orgName) throws DAOException {
		GroupProfile profile=new GroupProfile();
		profile.setDisplayName(orgName);
		String authorityName=create(repoDao,orgName,profile);
		return GroupDao.getGroup(repoDao, authorityName);
	}
	/**
	 * returns Groupname
	 * @param repoDao
	 * @param orgName
	 * @param profile
	 * @return
	 * @throws DAOException
	 */
	public static String create(RepositoryDao repoDao, String orgName, GroupProfile profile) throws DAOException {
		try {
			OrganizationService organizationService = OrganizationServiceFactory.getOrganizationService(repoDao.getApplicationInfo().getAppId());
			return organizationService.createOrganization(orgName, profile.getDisplayName());
		} catch (Throwable t) {
			throw DAOException.mapping(t);
		}		
	}
	public static OrganizationDao get(RepositoryDao repoDao, String groupName) throws DAOException {

		try {
			
			String currentUser = AuthenticationUtil.getFullyAuthenticatedUser(); 
			
			List<EduGroup> groups = getOrganizations(repoDao);
			for (EduGroup eduGroup : groups) {
				
				String eduGroupName = generateGroupName(eduGroup);
				
				if (! eduGroupName.equals(groupName)) {
					
					continue;
				}
				OrganizationDao org=new OrganizationDao(repoDao, eduGroup);
				
				return org;
			}
			
			throw new DAOMissingException(
					new IllegalArgumentException(groupName));
			
		} catch (Throwable e) {

			throw DAOException.mapping(e);
		}
				
	}

	private RepositoryDao repoDao;	
	private final EduGroup eduGroup;
	
	private final String authorityName;
	private final String groupName;
	
	public OrganizationDao(RepositoryDao repoDao, EduGroup eduGroup) {

		this.repoDao = repoDao;
		this.eduGroup = eduGroup;		
		
		this.authorityName = generateAuthorityName(eduGroup);		
		this.groupName = generateGroupName(eduGroup);
		
	}
	/**
	 * returns true if the user is allowed to administer this org
	 * @return
	 */
	public boolean hasAdministrationAccess(){
		return AuthorityServiceFactory.getAuthorityService(repoDao.getId()).hasAdminAccessToOrganization(groupName);
	}

	public Organization asOrganization() {
		
		Organization data = new Organization();
    			
    	data.setAuthorityName(authorityName);
    	data.setAuthorityType(Authority.Type.GROUP);
    	data.setGroupName(groupName);
    	data.setAdministrationAccess(hasAdministrationAccess());
    	
    	GroupProfile profile = new GroupProfile();
    	profile.setDisplayName(eduGroup.getGroupDisplayName());
    	data.setProfile(profile);
    	
    	NodeRef ref = new NodeRef();
    	ref.setRepo(repoDao.getId());
    	ref.setId(eduGroup.getFolderId());
    	data.setSharedFolder(ref);
    	    	
    	return data; 
	}

	public void delete() throws DAOException {

		try {
			
			String currentUser = AuthenticationUtil.getFullyAuthenticatedUser(); 
			
			if (!repoDao.getBaseClient().isAdmin(currentUser)) {
								
				throw new AccessDeniedException(currentUser);
			}
				
			((MCAlfrescoAPIClient)repoDao.getBaseClient()).unbindEduGroupFolder(groupName, eduGroup.getFolderId());
			
		} catch (Exception e) {

			throw DAOException.mapping(e);
		}
				
	}

	private static String generateAuthorityName(EduGroup eduGroup) {
		
		String groupName = eduGroup.getGroupname();
		
		return    groupName.startsWith(PermissionService.GROUP_PREFIX) 
				? groupName
				: PermissionService.GROUP_PREFIX + groupName;

	}
	
	private static String generateGroupName(EduGroup eduGroup) {
		
		String groupName = eduGroup.getGroupname();
		
		return    groupName.startsWith(PermissionService.GROUP_PREFIX) 
				? groupName.substring(PermissionService.GROUP_PREFIX.length())
				: groupName;

	}
	public void removeMember(String member) throws DAOException{
		if(!hasAdministrationAccess())
			throw new DAOSecurityException(new Throwable("no administration access for org "+groupName));
		AuthenticationUtil.runAsSystem(new RunAsWork<Void>() {

			@Override
			public Void doWork() throws Exception {
				// will throw if member is invalid user
				repoDao.getBaseClient().getUserInfo(member).get(CCConstants.CM_PROP_PERSON_USERNAME);
				removeMember(groupName, member);
				return null;
			}
		});		
	}
	private void removeMember(String groupName,String authorityName) throws DAOException {
		String[] members=((MCAlfrescoAPIClient)repoDao.getBaseClient()).getMemberships(groupName);
		for(String auth : members){
			if(auth.startsWith(PermissionService.GROUP_PREFIX)){
				removeMember(auth.substring(PermissionService.GROUP_PREFIX.length()),authorityName);
			}
			else if(auth.equals(authorityName)){
				((MCAlfrescoAPIClient)repoDao.getBaseClient()).removeMemberships(groupName, new String[]{authorityName});
			}
		}		
	}

}
