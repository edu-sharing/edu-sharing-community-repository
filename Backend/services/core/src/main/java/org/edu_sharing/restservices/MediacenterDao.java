package org.edu_sharing.restservices;

import com.google.gson.Gson;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.permissions.AccessDeniedException;
import org.alfresco.service.cmr.security.PermissionService;
import org.edu_sharing.alfresco.service.AuthorityService;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.restservices.shared.Authority;
import org.edu_sharing.restservices.shared.Mediacenter;
import org.edu_sharing.service.authority.AuthorityServiceHelper;
import org.edu_sharing.service.mediacenter.MediacenterService;
import org.edu_sharing.service.model.NodeRef;
import org.edu_sharing.service.model.NodeRefImpl;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MediacenterDao extends AbstractDao{

	private String authorityName;
	private NodeRef nodeRef;

	MediacenterService mediacenterService;

	public MediacenterDao(RepositoryDao repoDao){
		super(repoDao);
		mediacenterService = this.repoDao.getMediacenterService();
	}
	public static MediacenterDao create(RepositoryDao repoDao,String name,Mediacenter.Profile profile) throws DAOException {
		try {
			return new MediacenterDao(repoDao).create(name,profile);
		} catch (Exception e) {
			throw DAOException.mapping(e);
		}
	}
	public boolean isAvailableForCurrentUser() throws Exception {
		if(AuthorityServiceHelper.isAdmin()) {
			return true;
		}
		return searchService.getAllMediacentersNodeRef(true).stream().anyMatch(
				m -> this.authorityName.equals(m.getProperties().get(CCConstants.CM_PROP_AUTHORITY_NAME))
		);
	}
	public static MediacenterDao get(RepositoryDao repoDao,String name) throws DAOException {
		try {
			return new MediacenterDao(repoDao).get(name);
		} catch (Exception e) {
			throw DAOException.mapping(e);
		}
	}

	public static void delete(RepositoryDao repoDao, String authorityName) throws DAOException {
		try {
			new MediacenterDao(repoDao).delete(authorityName);
		} catch (Exception e) {
			throw DAOException.mapping(e);
		}
	}

	private void delete(String authorityName){
		//check and throw if not allowed
		if (!org.edu_sharing.service.authority.AuthorityServiceFactory.getInstance().getLocalService().isGlobalAdmin()) {
			throw new RuntimeException("You need global admin rights.");
		}
		mediacenterService.deleteMediacenter(authorityName);
	}

	public static List<MediacenterDao> getAll(RepositoryDao repoDao) throws DAOException {
		return new MediacenterDao(repoDao).getAll();
	}

	private List<MediacenterDao> getAll() throws DAOException {
		try {
			return searchService.getAllMediacentersNodeRef(false).stream().map((authority)-> {
				try {
					return new MediacenterDao(repoDao).get(authority);
				} catch (DAOException e) {
					throw new RuntimeException(e);
				}
			}).collect(Collectors.toList());
		} catch (Exception e) {
			throw DAOException.mapping(e);
		}

	}

	public void checkAdminAccess() {
		if(!authorityService.hasAdminAccessToMediacenter(this.authorityName)){
			throw new AccessDeniedException("User does not have permissions to manage this group");
		}
	}

	public void addManagedGroup(String group) throws DAOException {
		try {
			authorityService.addMemberships(this.authorityName, List.of(group));
		}catch(Throwable t){
			throw DAOException.mapping(t);
		}
	}
	public void removeManagedGroup(String group) throws DAOException {
		try {
			authorityService.removeMemberships(this.authorityName, List.of(group));
		}catch(Throwable t){
			throw DAOException.mapping(t);
		}
	}
	public Mediacenter asMediacenter() {
		try {
			String groupName = authorityName.startsWith(PermissionService.GROUP_PREFIX)
					? authorityName
					: PermissionService.GROUP_PREFIX + authorityName;
			Mediacenter mediacenter = new Mediacenter(groupName,
					Authority.Type.GROUP
			);
			// extend the group profile with mediacenter data
			Mediacenter.Profile profile = new Mediacenter.Profile();
			profile.setDisplayName(new MCAlfrescoAPIClient().getGroupDisplayName(authorityName));
			// this is verified on init of mediacenter so there is no need to fetch it again
			profile.setGroupType(AuthorityService.MEDIA_CENTER_GROUP_TYPE);
			profile.setScopeType((String) NodeServiceHelper.getPropertyFromNodeRef(nodeRef, CCConstants.CCM_PROP_SCOPE_TYPE));
			profile.setGroupEmail((String) NodeServiceHelper.getPropertyFromNodeRef(nodeRef, CCConstants.CCM_PROP_GROUPEXTENSION_GROUPEMAIL));
			Mediacenter.MediacenterProfileExtension mProfile=new Mediacenter.MediacenterProfileExtension();
			mProfile.setId((String) NodeServiceHelper.getPropertyFromNodeRef(nodeRef, CCConstants.CCM_PROP_MEDIACENTER_ID));
			try {
				boolean isActive = mediacenterService.isActive(mediacenter.getAuthorityName());
				mProfile.setContentStatus((isActive) ? Mediacenter.MediacenterProfileExtension.ContentStatus.Activated : Mediacenter.MediacenterProfileExtension.ContentStatus.Deactivated);
			}catch(NullPointerException t){}
			mProfile.setLocation((String) NodeServiceHelper.getPropertyFromNodeRef(nodeRef, CCConstants.CCM_PROP_ADDRESS_CITY));
			mProfile.setDistrictAbbreviation((String) NodeServiceHelper.getPropertyFromNodeRef(nodeRef, CCConstants.CCM_PROP_MEDIACENTER_DISTRICT_ABBREVIATION));
			mProfile.setMainUrl((String) NodeServiceHelper.getPropertyFromNodeRef(nodeRef, CCConstants.CCM_PROP_MEDIACENTER_MAIN_URL));
			try {
				mProfile.setCatalogs(Arrays.asList(new Gson().fromJson((String) NodeServiceHelper.getPropertyFromNodeRef(nodeRef, CCConstants.CCM_PROP_MEDIACENTER_CATALOGS),
						Mediacenter.Catalog[].class)));
			}catch(NullPointerException e){}
			profile.setMediacenter(mProfile);
			mediacenter.setProfile(profile);

			mediacenter.setAdministrationAccess(authorityService.hasAdminAccessToMediacenter(groupName));
			return mediacenter;
		}catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
	private MediacenterDao get(org.edu_sharing.service.model.NodeRef nodeRef) throws DAOException {
		String property = (String) NodeServiceHelper.getPropertyFromNodeRef(nodeRef, CCConstants.CCM_PROP_GROUPEXTENSION_GROUPTYPE);
		this.nodeRef=nodeRef;
		if(property==null || !property.equals(AuthorityService.MEDIA_CENTER_GROUP_TYPE)) {
			throw new java.lang.IllegalArgumentException("The given authority is not of type " + AuthorityService.MEDIA_CENTER_GROUP_TYPE);
		}
		this.authorityName= (String) nodeRef.getProperties().get(CCConstants.CM_PROP_AUTHORITY_NAME);
		return this;
	}

	private MediacenterDao get(String group) throws DAOException {
		try {
			org.alfresco.service.cmr.repository.NodeRef alfRef = authorityService.getAuthorityNodeRef(group);
			if(alfRef == null) {
				throw new DAOMissingException(new Exception("Authority not found: "+group));
			}
			nodeRef = new NodeRefImpl(alfRef);
			String property = (String) NodeServiceHelper.getPropertyNative(alfRef, CCConstants.CCM_PROP_GROUPEXTENSION_GROUPTYPE);
			if(property==null || !property.equals(AuthorityService.MEDIA_CENTER_GROUP_TYPE)) {
				throw new IllegalArgumentException("The given authority is not of type "+AuthorityService.MEDIA_CENTER_GROUP_TYPE);
			}

			this.authorityName=group;
			return this;
		} catch (Exception e) {
			throw DAOException.mapping(e);
		}
	}
	public List<GroupDao> getManagedGroups() {
		try {
			//check and throw if not allowed
			mediacenterService.isAllowedToManage(authorityName);

			return authorityService.getMembershipsOfGroup(this.authorityName).stream().filter((group) -> group.startsWith(PermissionService.GROUP_PREFIX)).map((group) -> {
						try {
							return GroupDao.getGroup(repoDao, group);
						} catch (DAOException e) {
							throw new RuntimeException(e);
						}
					}).
					filter((group) -> !AuthorityService.MEDIACENTER_ADMINISTRATORS_GROUP_TYPE.equals(group.getGroupType())).
					collect(Collectors.toList());
		} catch (Throwable t) {
			throw DAOException.mapping(t);
		}
	}


	public void changeProfile(Mediacenter.Profile profile) throws DAOException {
		try {
			//check and throw if not allowed
			mediacenterService.isAllowedToManage(authorityName);

			boolean active = (profile.getMediacenter().getContentStatus()==null
					|| profile.getMediacenter().getContentStatus().equals(Mediacenter.MediacenterProfileExtension.ContentStatus.Deactivated)) ? false : true;
			AuthenticationUtil.runAsSystem(() -> {
				mediacenterService.updateMediacenter(authorityName, profile.getDisplayName(), null,
						profile.getMediacenter().getLocation(), profile.getMediacenter().getDistrictAbbreviation(),
						profile.getMediacenter().getMainUrl(), new Gson().toJson(profile.getMediacenter().getCatalogs()), active);
				return null;
			});
		}catch(Throwable t){
			throw DAOException.mapping(t);
		}
	}
	private MediacenterDao create(String name,Mediacenter.Profile profile) throws DAOException {
		try {
			this.authorityName = mediacenterService.createMediacenter(name,profile.getDisplayName(),null,null);
			nodeRef = new NodeRefImpl(authorityService.getAuthorityNodeRef(authorityName));
			if(profile.getMediacenter().getContentStatus()==null
					|| profile.getMediacenter().getContentStatus().equals(Mediacenter.MediacenterProfileExtension.ContentStatus.Deactivated)){
				mediacenterService.setActive(false, authorityName );
			}else if(profile.getMediacenter().getContentStatus().equals(Mediacenter.MediacenterProfileExtension.ContentStatus.Activated)){
				mediacenterService.setActive(true, authorityName );
			}
			return this;
		}catch(Throwable t){
			throw DAOException.mapping(t);
		}
	}

	public org.edu_sharing.service.model.NodeRef getNodeRef() {
		return nodeRef;
	}
}
