package org.edu_sharing.service.permission;

import java.util.*;
import java.util.stream.Collectors;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.edu_sharing.repository.client.rpc.ACE;
import org.edu_sharing.repository.client.rpc.ACL;
import org.edu_sharing.repository.client.tools.CCConstants;

public class PermissionServiceHelper {
	public static final String[] PERMISSIONS = new String[] {
			 org.alfresco.service.cmr.security.PermissionService.READ,
			 CCConstants.PERMISSION_READ_PREVIEW,
			 CCConstants.PERMISSION_READ_ALL,
			 CCConstants.PERMISSION_COMMENT,
			 CCConstants.PERMISSION_FEEDBACK,
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

    public static HashSet<String> getExplicitAuthoritiesFromACL(ACL acl) {
        return Arrays.stream(acl.getAces()).
                filter((ace) -> !ace.isInherited()).
                map(ACE::getAuthority).
                collect(Collectors.toCollection(HashSet::new));
    }

	public void validatePermissionOrThrow(String nodeId, String permissionName) {
		if(!permissionService.hasPermission(StoreRef.PROTOCOL_WORKSPACE,StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), nodeId,permissionName))
			throw new PermissionException(nodeId,permissionName);
	}

	public HashMap<String, Boolean> hasAllPermissions(String storeProtocol,String storeId,String nodeId){
			return permissionService.hasAllPermissions(storeProtocol, storeId, nodeId, PERMISSIONS);
		}
		public HashMap<String, Boolean> hasAllPermissions(String nodeId){
			return hasAllPermissions(StoreRef.PROTOCOL_WORKSPACE,StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), nodeId);
		}
		public static List<String> getPermissionsAsString(Map<String,Boolean> hasPermissions){
			List<String> result = new ArrayList<String>();

			for (String permission : PERMISSIONS) {

				if(hasPermissions.get(permission) == null || 
						 !hasPermissions.get(permission))
					continue;
				result.add(permission);
			}

			return result;
			
		}
	public static boolean hasPermission(NodeRef nodeRef, String permission){
			return PermissionServiceFactory.getLocalService().hasPermission(nodeRef.getStoreRef().getProtocol(),
					nodeRef.getStoreRef().getIdentifier(),
					nodeRef.getId(),
					permission);
	}
}
