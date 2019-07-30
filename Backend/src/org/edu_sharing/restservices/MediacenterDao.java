package org.edu_sharing.restservices;

import com.google.gson.Gson;
import org.alfresco.service.cmr.security.PermissionService;
import org.edu_sharing.alfresco.service.AuthorityService;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.restservices.shared.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MediacenterDao extends AbstractDao{

	private String authorityName;

	public MediacenterDao(RepositoryDao repoDao){
		super(repoDao);
	}
	public static MediacenterDao create(RepositoryDao repoDao,String name,Mediacenter.Profile profile) throws DAOException {
		try {
			return new MediacenterDao(repoDao).create(name,profile);
		} catch (Exception e) {
			throw DAOException.mapping(e);
		}
	}
	public static MediacenterDao get(RepositoryDao repoDao,String name) throws DAOException {
		try {
			return new MediacenterDao(repoDao).get(name);
		} catch (Exception e) {
			throw DAOException.mapping(e);
		}
	}

	public static List<MediacenterDao> getAll(RepositoryDao repoDao) throws DAOException {
		return new MediacenterDao(repoDao).getAll();
	}

	private List<MediacenterDao> getAll() throws DAOException {
		try {
			return searchService.getAllMediacenters().stream().map((authority)-> {
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

	public void addManagedGroup(String group) throws DAOException {
		try {
			authorityService.addMemberships(this.authorityName, new String[]{group});
		}catch(Throwable t){
			throw DAOException.mapping(t);
		}
	}
	public void removeManagedGroup(String group) throws DAOException {
		try {
			authorityService.removeMemberships(this.authorityName, new String[]{group});
		}catch(Throwable t){
			throw DAOException.mapping(t);
		}
	}
	public Mediacenter asMediacenter() {
		try {
			GroupDao groupDao = GroupDao.getGroup(repoDao, authorityName);
			Mediacenter mediacenter = new Mediacenter(groupDao.asGroup());
			// extend the group profile with mediacenter data
			Mediacenter.Profile profile = new Mediacenter.Profile(mediacenter.getProfile());
			Mediacenter.MediacenterProfileExtension mProfile=new Mediacenter.MediacenterProfileExtension();
			mProfile.setId(authorityService.getProperty(mediacenter.getAuthorityName(), CCConstants.CCM_PROP_MEDIACENTER_ID));
			try {
				mProfile.setContentStatus(Mediacenter.MediacenterProfileExtension.ContentStatus.valueOf(authorityService.getProperty(mediacenter.getAuthorityName(), CCConstants.CCM_PROP_MEDIACENTER_CONTENT_STATUS)));
			}catch(NullPointerException t){}
			mProfile.setLocation(authorityService.getProperty(mediacenter.getAuthorityName(), CCConstants.CCM_PROP_MEDIACENTER_LOCATION));
			mProfile.setDistrictAbbreviation(authorityService.getProperty(mediacenter.getAuthorityName(), CCConstants.CCM_PROP_MEDIACENTER_DISTRICT_ABBREVIATION));
			mProfile.setMainUrl(authorityService.getProperty(mediacenter.getAuthorityName(), CCConstants.CCM_PROP_MEDIACENTER_MAIN_URL));
			try {
				mProfile.setCatalogs(Arrays.asList(new Gson().fromJson(authorityService.getProperty(mediacenter.getAuthorityName(), CCConstants.CCM_PROP_MEDIACENTER_CATALOGS),
						Mediacenter.Catalog[].class)));
			}catch(NullPointerException e){}
			profile.setMediacenter(mProfile);
			mediacenter.setProfile(profile);
			mediacenter.setAdministrationAccess(authorityService.hasAdminAccessToMediacenter(authorityName));
			return mediacenter;
		}catch(DAOException e){
			throw new RuntimeException(e);
		}
	}

	private MediacenterDao get(String group) throws DAOException {
		try {
			org.alfresco.service.cmr.repository.NodeRef ref=authorityService.getAuthorityNodeRef(group);
			if(ref==null)
				throw new DAOMissingException(new Exception("Authority not found: "+group));
			String property = nodeService.getProperty(ref.getStoreRef().getProtocol(), ref.getStoreRef().getIdentifier(), ref.getId(), CCConstants.CCM_PROP_GROUPEXTENSION_GROUPTYPE);
			if(property==null || !property.equals(CCConstants.MEDIA_CENTER_GROUP_TYPE))
				throw new java.lang.IllegalArgumentException("The given authority is not of type "+CCConstants.MEDIA_CENTER_GROUP_TYPE);

			this.authorityName=group;
			return this;
		} catch (Exception e) {
			throw DAOException.mapping(e);
		}
	}
	public List<GroupDao> getManagedGroups(){
		return Arrays.stream(authorityService.getMembershipsOfGroup(this.authorityName)).filter((group)->group.startsWith(PermissionService.GROUP_PREFIX)).map((group)-> {
			try {
				return GroupDao.getGroup(repoDao,group);
			} catch (DAOException e) {
				throw new RuntimeException(e);
			}
		}).
				filter((group)->!AuthorityService.MEDIACENTER_ADMINISTRATORS_GROUP_TYPE.equals(group.getGroupType())).
				collect(Collectors.toList());
	}
	public void changeProfile(Mediacenter.Profile profile) throws DAOException {
		// always force the group type to media center
		profile.setGroupType(CCConstants.MEDIA_CENTER_GROUP_TYPE);
		// first, change the basic profile (admin access is checked there)
		GroupDao.getGroup(repoDao,authorityName).changeProfile(profile);
		// then, change the mediacenter releated data
		authorityService.addAuthorityAspect(authorityName,CCConstants.CCM_ASPECT_MEDIACENTER);
		if(profile.getMediacenter()!=null) {
			authorityService.setAuthorityProperty(authorityName, CCConstants.CCM_PROP_MEDIACENTER_ID, profile.getMediacenter().getId());
			authorityService.setAuthorityProperty(authorityName, CCConstants.CCM_PROP_MEDIACENTER_CONTENT_STATUS,
					profile.getMediacenter().getContentStatus()==null ? Mediacenter.MediacenterProfileExtension.ContentStatus.Deactivated.toString() : profile.getMediacenter().getContentStatus().toString()
			);
			authorityService.setAuthorityProperty(authorityName, CCConstants.CCM_PROP_MEDIACENTER_LOCATION, profile.getMediacenter().getLocation());
			authorityService.setAuthorityProperty(authorityName, CCConstants.CCM_PROP_MEDIACENTER_DISTRICT_ABBREVIATION, profile.getMediacenter().getDistrictAbbreviation());
			authorityService.setAuthorityProperty(authorityName, CCConstants.CCM_PROP_MEDIACENTER_MAIN_URL, profile.getMediacenter().getMainUrl());
			authorityService.setAuthorityProperty(authorityName, CCConstants.CCM_PROP_MEDIACENTER_CATALOGS, new Gson().toJson(profile.getMediacenter().getCatalogs()));
		}
	}
	private MediacenterDao create(String name,Mediacenter.Profile profile) throws DAOException {
		try {
			profile.setGroupType(CCConstants.MEDIA_CENTER_GROUP_TYPE);
			String group = GroupDao.createGroup(repoDao, name, profile, null).getAuthorityName();
			this.authorityName = group;
			changeProfile(profile);

			// create the admin group
			authorityService.createGroupWithType(AuthorityService.ADMINISTRATORS_GROUP, profile.getDisplayName() + AuthorityService.ADMINISTRATORS_GROUP_DISPLAY_POSTFIX, group, AuthorityService.MEDIACENTER_ADMINISTRATORS_GROUP_TYPE);
			return this;
		}catch(Throwable t){
			throw DAOException.mapping(t);
		}
	}
}
