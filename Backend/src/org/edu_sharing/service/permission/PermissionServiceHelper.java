package org.edu_sharing.service.permission;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.alfresco.service.cmr.repository.StoreRef;
import org.edu_sharing.repository.client.tools.CCConstants;

public class PermissionServiceHelper {
	public static final String[] PERMISSIONS = new String[] {
			 org.alfresco.service.cmr.security.PermissionService.READ,
			 CCConstants.PERMISSION_READ_PREVIEW,
			 CCConstants.PERMISSION_READ_ALL,
			 CCConstants.PERMISSION_COMMENT,
			 org.alfresco.service.cmr.security.PermissionService.ADD_CHILDREN,
			 org.alfresco.service.cmr.security.PermissionService.READ_PERMISSIONS,
			 org.alfresco.service.cmr.security.PermissionService.CHANGE_PERMISSIONS,
			 org.alfresco.service.cmr.security.PermissionService.READ_CHILDREN,
			 org.alfresco.service.cmr.security.PermissionService.DELETE_CHILDREN,
			 org.alfresco.service.cmr.security.PermissionService.WRITE,
			 org.alfresco.service.cmr.security.PermissionService.DELETE,
			 CCConstants.PERMISSION_CC_PUBLISH};
	private PermissionService permissionService;
		public PermissionServiceHelper(PermissionService permissionService){
			this.permissionService=permissionService;
		}
		public HashMap<String, Boolean> hasAllPermissions(String storeProtocol,String storeId,String nodeId){
			return permissionService.hasAllPermissions(storeProtocol, storeId, nodeId, PERMISSIONS);
		}
		public HashMap<String, Boolean> hasAllPermissions(String nodeId){
			return hasAllPermissions(StoreRef.PROTOCOL_WORKSPACE,StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), nodeId);
		}
		public static List<String> getPermissionsAsString(HashMap<String,Boolean> hasPermissions){
			List<String> result = new ArrayList<String>();

			for (String permission : PERMISSIONS) {

				if(hasPermissions.get(permission) == null || 
						 !hasPermissions.get(permission))
					continue;
				result.add(permission);
			}

			return result;
			
		}
	
}
