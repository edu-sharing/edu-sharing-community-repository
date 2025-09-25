package org.edu_sharing.service.permission;

import org.edu_sharing.repository.client.tools.CCConstants;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Lazy
@Service
public class PermissionServiceReadOnly extends PermissionServiceAdapter {

	public PermissionServiceReadOnly() {
		ALLOWED_PERMISSIONS.add(CCConstants.PERMISSION_READ);
		ALLOWED_PERMISSIONS.add(CCConstants.PERMISSION_READ_PREVIEW);
		ALLOWED_PERMISSIONS.add(CCConstants.PERMISSION_READ_ALL);
	}
	
}
