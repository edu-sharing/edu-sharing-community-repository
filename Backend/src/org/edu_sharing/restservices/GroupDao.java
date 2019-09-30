package org.edu_sharing.restservices;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.security.permissions.AccessDeniedException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.PermissionService;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.MCAlfrescoBaseClient;
import org.edu_sharing.repository.server.tools.cache.PersonCache;
import org.edu_sharing.restservices.shared.Authority;
import org.edu_sharing.restservices.shared.Group;
import org.edu_sharing.restservices.shared.GroupProfile;
import org.edu_sharing.service.authority.AuthorityService;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.service.organization.OrganizationService;
import org.edu_sharing.service.organization.OrganizationServiceFactory;

public class GroupDao {

	public static GroupDao getGroup(RepositoryDao repoDao, String groupName) throws DAOException {
		
		try {
			
			return new GroupDao(repoDao, groupName);
			
		} catch (Exception e) {

			throw DAOException.mapping(e);
		}
	}

	public static GroupDao createGroup(RepositoryDao repoDao, String groupName, GroupProfile profile,String parentGroup) throws DAOException {
		try {
			AuthorityService authorityService = AuthorityServiceFactory.getAuthorityService(repoDao.getApplicationInfo().getAppId());
			String result=authorityService.createGroup(groupName, profile.getDisplayName(), parentGroup);
			GroupDao groupDao=GroupDao.getGroup(repoDao, result);
			if(result!=null) {
				// permission check was done already, so run as system to allow org admin to set properties
				AuthenticationUtil.runAsSystem(()-> {
					groupDao.setGroupEmail(profile);
					groupDao.setGroupType(profile);
					groupDao.setScopeType(profile);
					return null;
				});
			}
			return groupDao;
		} catch (Exception e) {
			throw DAOException.mapping(e);
		}
	}
	
	public static List<GroupDao> search(RepositoryDao repoDao, String pattern) throws DAOException {

		try {
			
			List<GroupDao> resultset = new ArrayList<GroupDao>();
			for (String groupName : ((MCAlfrescoAPIClient)repoDao.getBaseClient()).searchGroupNames(pattern)) {
				
				resultset.add(new GroupDao(repoDao, groupName));
			}
					
			return resultset;
			
		} catch (Exception e) {

			throw DAOException.mapping(e);
		}
	}

	private final MCAlfrescoBaseClient baseClient;

	private final RepositoryDao repoDao;
	
	private final String authorityName;
	private final String groupName;
	private final String displayName;

	private AuthorityService authorityService;

	private String groupType;

	private String groupEmail;

	private NodeRef ref;

	public GroupDao(RepositoryDao repoDao, String groupName) throws DAOException  {

		try {
			
			this.baseClient = repoDao.getBaseClient();
			this.authorityService = AuthorityServiceFactory.getAuthorityService(repoDao.getApplicationInfo().getAppId());
			this.repoDao = repoDao;					

			this.authorityName = 
					  groupName.startsWith(PermissionService.GROUP_PREFIX) 
					? groupName
					: PermissionService.GROUP_PREFIX + groupName;
			
			this.groupName = 
					  groupName.startsWith(PermissionService.GROUP_PREFIX) 
					? groupName.substring(PermissionService.GROUP_PREFIX.length())
					: groupName;
								
			this.displayName = ((MCAlfrescoAPIClient)baseClient).getGroupDisplayName(this.groupName);
			if (displayName == null) {
				
				throw new DAOMissingException(
						new IllegalArgumentException(groupName));
				
			}
			this.groupType= authorityService.getProperty(this.authorityName,CCConstants.CCM_PROP_GROUPEXTENSION_GROUPTYPE);

			this.groupEmail= authorityService.getProperty(this.authorityName,CCConstants.CCM_PROP_GROUPEXTENSION_GROUPEMAIL);
			this.ref = authorityService.getAuthorityNodeRef(this.authorityName);
			
		} catch (Throwable t) {
			
			throw DAOException.mapping(t);
		}
	}
	
	public void changeProfile(GroupProfile profile) throws DAOException {
		
		try {
			checkModifyAccess();
			AuthenticationUtil.runAsSystem(new RunAsWork<Void>() {

				@Override
				public Void doWork() throws Exception {
					((MCAlfrescoAPIClient)repoDao.getBaseClient()).createOrUpdateGroup(groupName, profile.getDisplayName());
					setGroupType(profile);
					setGroupEmail(profile);
					setScopeType(profile);
					return null;
				}
			});
			
		} catch (Throwable t) {
			
			throw DAOException.mapping(t);
		}

	}
	
	protected void setGroupType(GroupProfile profile) {
		if(profile.getGroupType()!=null){
			authorityService.addAuthorityAspect(PermissionService.GROUP_PREFIX+groupName, CCConstants.CCM_ASPECT_GROUPEXTENSION);
			authorityService.setAuthorityProperty(PermissionService.GROUP_PREFIX+groupName, CCConstants.CCM_PROP_GROUPEXTENSION_GROUPTYPE,profile.getGroupType());
		}

	}
	protected void setGroupEmail(GroupProfile profile) {
		authorityService.setAuthorityProperty(PermissionService.GROUP_PREFIX+groupName,CCConstants.CCM_PROP_GROUPEXTENSION_GROUPEMAIL,profile.getGroupEmail());

	}
	protected void setScopeType(GroupProfile profile) {
		if(profile.getScopeType()!=null){
			authorityService.addAuthorityAspect(PermissionService.GROUP_PREFIX+groupName, CCConstants.CCM_ASPECT_SCOPE);
			authorityService.setAuthorityProperty(PermissionService.GROUP_PREFIX+groupName, CCConstants.CCM_PROP_SCOPE_TYPE,profile.getScopeType());
		}

	}

	public void delete() throws DAOException {
		
		try {
			checkModifyAccess();
			AuthenticationUtil.runAsSystem(new RunAsWork<Void>() {
				@Override
				public Void doWork() throws Exception {
					authorityService.deleteAuthority(PermissionService.GROUP_PREFIX+groupName);
					return null;
				}
			});
			
		} catch (Exception e) {

			throw DAOException.mapping(e);
		}
	}

	public void addMember(String member) throws DAOException {
		
		try {
			checkAdminAccess();
			AuthenticationUtil.runAsSystem(new RunAsWork<Void>() {

				@Override
				public Void doWork() throws Exception {
					authorityService.addMemberships(groupName, new String[]{member});
					PersonCache.reset(member);
					return null;
				}
			});
			
		} catch (Exception e) {

			throw DAOException.mapping(e);
		}
	}

	public void checkAdminAccess() {
		if(!authorityService.hasAdminAccessToGroup(groupName)){
			throw new AccessDeniedException("User does not have permissions to manage this group");
		}
	}
	public void checkModifyAccess() {
		if(!authorityService.hasModifyAccessToGroup(groupName)){
			throw new AccessDeniedException("User does not have permissions to modify this group");
		}
	}
	public void deleteMember(String member) throws DAOException {
		
		try {
			checkAdminAccess();
			AuthenticationUtil.runAsSystem(new RunAsWork<Void>() {

				@Override
				public Void doWork() throws Exception {
					authorityService.removeMemberships(groupName, new String[]{member});
					PersonCache.reset(member);
					return null;
				}
			});
			
		} catch (Exception e) {

			throw DAOException.mapping(e);
		}
	}

	public List<Authority> getMember() throws DAOException {

		List<Authority> result = new ArrayList<Authority>();

		try {

			checkAdminAccess();
						
			for (String member : authorityService.getMemberships(groupName)) {
				
				result.add(   member.startsWith(PermissionService.GROUP_PREFIX) 
							? GroupDao.getGroup(repoDao, member).asGroup()
							: PersonDao.getPerson(repoDao, member).asPerson());
							
			}
			
		} catch (Exception e) {

			throw DAOException.mapping(e);
		}
		
		return result;
		
	}

	
	public Group asGroup() {
		
    	Group data = new Group();
    	
    	data.setRef(getRef());
    	data.setAuthorityName(getAuthorityName());
    	data.setAuthorityType(Authority.Type.GROUP);
    	
    	data.setGroupName(getGroupName());

    	GroupProfile profile = new GroupProfile();
    	profile.setDisplayName(getDisplayName());
    	profile.setGroupType(getGroupType());
    	profile.setGroupEmail(getGoupEmail());
    	data.setProfile(profile);
    	
    	return data;
	}
	
	public String getGroupType() {
		return this.groupType;
	}
	private String getGoupEmail() {
		return this.groupEmail;
	}
	public org.edu_sharing.restservices.shared.NodeRef getRef() {

		return NodeDao.createNodeRef(repoDao, this.ref.getId());
	}

	public String getAuthorityName() {
	
		return this.authorityName;
	}
	
	public String getGroupName() {
		
		return this.groupName;
	}
	
	public String getDisplayName() {
		
		return this.displayName;
	}
	
}
