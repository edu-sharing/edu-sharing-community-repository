package org.edu_sharing.service.permission;

import java.lang.String;
import java.util.ArrayList;

import org.edu_sharing.repository.client.tools.CCConstants;


public class PermissionServiceReadOnly extends PermissionServiceAdapter {

	public PermissionServiceReadOnly(String appid) {
		ALLOWED_PERMISSIONS.add(CCConstants.PERMISSION_READ);
		ALLOWED_PERMISSIONS.add(CCConstants.PERMISSION_READ_PREVIEW);
		ALLOWED_PERMISSIONS.add(CCConstants.PERMISSION_READ_ALL);
	}
	
}
