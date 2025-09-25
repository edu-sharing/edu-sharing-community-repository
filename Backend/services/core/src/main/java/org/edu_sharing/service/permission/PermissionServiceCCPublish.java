package org.edu_sharing.service.permission;

import org.edu_sharing.repository.client.tools.CCConstants;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Lazy
@Service
public class PermissionServiceCCPublish extends PermissionServiceReadOnly {
	
	public PermissionServiceCCPublish() {
		super();
		ALLOWED_PERMISSIONS.add(CCConstants.PERMISSION_CC_PUBLISH);
	}
}
