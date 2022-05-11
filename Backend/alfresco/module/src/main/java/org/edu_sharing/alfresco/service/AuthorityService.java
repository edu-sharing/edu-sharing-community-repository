package org.edu_sharing.alfresco.service;

import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.util.MD5;
import org.apache.log4j.Logger;

public class AuthorityService {

	public static final String MEDIA_CENTER_GROUP_TYPE = "MEDIA_CENTER";
	public static final String MEDIA_CENTER_PROXY_GROUP_TYPE = "MEDIA_CENTER_PROXY";
	public static final String MEDIA_CENTER_PROXY_DISPLAY_POSTFIX = "_Proxy";
	
	public static String MEDIACENTER_ADMINISTRATORS_GROUP = "MEDIACENTER_ADMINISTRATORS";
	public static String ADMINISTRATORS_GROUP_TYPE = "ORG_ADMINISTRATORS";
	public static String MEDIACENTER_ADMINISTRATORS_GROUP_TYPE = "MEDIACENTER_ADMINISTRATORS";

	public static String ADMINISTRATORS_GROUP = "ORG_ADMINISTRATORS";
	public static String ORG_GROUP_PREFIX = "ORG_";
	public static String ADMINISTRATORS_GROUP_DISPLAY_POSTFIX = "_Admins";

	Logger logger = Logger.getLogger(AuthorityService.class);

	

	org.alfresco.service.cmr.security.AuthorityService authorityService;

	public String createOrUpdateGroup(String groupName, String displayName, String parentGroup,
			boolean preventDuplicate) {
		String originalName = AuthorityService.getGroupName(groupName, parentGroup);
		String name = originalName;
		String key = PermissionService.GROUP_PREFIX + name;
		
		if (preventDuplicate) {
			int i = 2;
			while (authorityService.authorityExists(key)) {
				name = originalName + "-" + i;
				key = PermissionService.GROUP_PREFIX + name;
				i++;
			}
		}
		
		if (authorityService.authorityExists(key)) {
			authorityService.setAuthorityDisplayName(key, displayName);
		} else {
			authorityService.createAuthority(AuthorityType.GROUP, name, displayName, authorityService.getDefaultZones());
			if (parentGroup != null) {
				addMemberships(parentGroup, new String[] { key });
			}
		}

		return name;
	}

	public void addMemberships(String groupName, String[] members) {

		String key = groupName.startsWith(PermissionService.GROUP_PREFIX ) ? groupName : PermissionService.GROUP_PREFIX + groupName;

		for (String member : members) {

			if (member == null) {
				continue;
			}

			authorityService.addAuthority(key, member);
		}

	}

	public static String getGroupName(String groupName, String parentGroup) {
		String prefix = "";
		if (parentGroup != null) {
			// strip group prefix if existing
			if(parentGroup.startsWith(PermissionService.GROUP_PREFIX)){
				parentGroup = parentGroup.substring(PermissionService.GROUP_PREFIX.length());
			}
			prefix = MD5.Digest(parentGroup.getBytes()) + "_";
		}
		String name = prefix + groupName;

		return name;
	}

	public void setAuthorityService(org.alfresco.service.cmr.security.AuthorityService authorityService) {
		this.authorityService = authorityService;
	}

}
