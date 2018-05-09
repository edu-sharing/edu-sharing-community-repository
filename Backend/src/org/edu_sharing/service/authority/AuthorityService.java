package org.edu_sharing.service.authority;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.MD5;
import org.edu_sharing.repository.client.rpc.EduGroup;
import org.edu_sharing.restservices.DAOException;

public interface AuthorityService {
	public static String ORGANIZATION_GROUP_FOLDER="EDU_SHARED";
	public static String ADMINISTRATORS_GROUP="ORG_ADMINISTRATORS";
	public static String ADMINISTRATORS_GROUP_TYPE="ORG_ADMINISTRATORS";
	public static String MEDIA_CENTER_GROUP_TYPE = "MEDIA_CENTER";
	public static String ORG_GROUP_PREFIX = "ORG_";
	public static String ADMINISTRATORS_GROUP_DISPLAY_POSTFIX = "_Admins";

	Set<String> getMemberships(String username) throws Exception;
	boolean isGlobalAdmin();
	boolean hasAdminAccessToOrganization(String orgName);
	void deleteAuthority(String authorityName);
	void setAuthorityProperty(String authority, String property, Serializable value);
	Object getAuthorityProperty(String authority, String property);
	void addAuthorityAspect(String authority, String aspect);
	/**
	 * Returns true if the current user is allowed to admister (mamange members) to this group
	 */
	boolean hasAdminAccessToGroup(String groupName);
	/**
	 * returns if the current user is allowed to modify this group
	 * Internal node: the difference between admin access and modify is: Modifying of the main org group is not allowed for an org admin
	 * @param groupName
	 * @return
	 */
	boolean hasModifyAccessToGroup(String groupName);
	
	/**
	 * 
	 * @return ALL edugroups of user
	 */
	public ArrayList<EduGroup> getAllEduGroups();
	
	/**
	 * 
	 * @return edugroups of current scope
	 */
	public ArrayList<EduGroup> getEduGroups();
	
	public ArrayList<EduGroup> getEduGroups(String scope);
	
	/**
	 * creates an edugroup with groupadministrators group in a scoped area
	 * 
	 * @param eduGroup data of the new edugroup
	 * @param unscopedEduGroup if it is a scoped group then use the use unscopedEduGroup to find the ORG_ADMINISTRATORS displayName and groupname
	 * if it is null no ORG_ADMINISTRATORS will be created
	 * @param folderParentId
	 * @return
	 */
	public EduGroup getOrCreateEduGroup(EduGroup eduGroup, EduGroup unscopedEduGroup, String folderParentId);
	boolean isGuest();
	public String getProperty(String authorityName, String ccmPropGroupextensionGrouptype);
	EduGroup getEduGroup(String authority);
	String createGroup(String groupName, String displayName, String parentGroup) throws DAOException;
	
	static String getGroupName(String groupName,String parentGroup){
		return org.edu_sharing.alfresco.service.AuthorityService.getGroupName(groupName, parentGroup);
	}
	boolean authorityExists(String authority);
	NodeRef getAuthorityNodeRef(String authority);

}
