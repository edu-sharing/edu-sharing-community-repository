package org.edu_sharing.service.permission;

import org.edu_sharing.repository.client.tools.CCConstants;

public class PermissionServiceCCPublish extends PermissionServiceReadOnly {
	
	public PermissionServiceCCPublish(String appId) {
		super(appId);
		ALLOWED_PERMISSIONS.add(CCConstants.PERMISSION_CC_PUBLISH);
	}
}
