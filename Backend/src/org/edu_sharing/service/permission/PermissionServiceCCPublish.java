package org.edu_sharing.service.permission;

import org.edu_sharing.repository.client.tools.CCConstants;

public class PermissionServiceCCPublish extends PermissionServiceAdapter {
	
	public PermissionServiceCCPublish(String appId) {
		ALLOWED_PERMISSIONS.add(CCConstants.PERMISSION_READ);
		ALLOWED_PERMISSIONS.add(CCConstants.PERMISSION_CC_PUBLISH);
	}
}
