/**
 *
 *  
 * 
 * 
 *	
 *
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 *
 */
package org.edu_sharing.alfresco;

import java.util.HashMap;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.PermissionService;
import org.apache.log4j.Logger;


public class HasPermissionsWork implements AuthenticationUtil.RunAsWork<java.util.HashMap<String,Boolean>>{
	
	Logger logger = Logger.getLogger( HasPermissionsWork.class);
	String userId = null;
	String[] permissions = null;
	String nodeId = null;
	
	StoreRef storeRef = new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore");
	
	PermissionService permissionService = null;
	
	public HasPermissionsWork(PermissionService _permissionService, String _userId, String[] _permissions, String _nodeId) {
		userId = _userId;
		permissions = _permissions;
		nodeId = _nodeId;
		permissionService = _permissionService;
	}
	@Override
	public java.util.HashMap<String,Boolean> doWork() throws Exception {
		HashMap<String,Boolean> result = new HashMap<String, Boolean>();
		for(String permission:permissions){
			
			logger.debug("NEWnodeId:"+nodeId);
			logger.debug("permission:"+permission);
			logger.debug("storeRef:"+storeRef);
			
			NodeRef nodeRef = new NodeRef(storeRef, nodeId);
			logger.debug("nodeRef:"+nodeRef);
			
			AccessStatus accessStatus = permissionService.hasPermission(nodeRef, permission);
			
			logger.debug("accessStatus:"+accessStatus);
			
			
			if(accessStatus.equals(AccessStatus.ALLOWED)){
				result.put(permission, new Boolean(true));
			}else{
				result.put(permission, new Boolean(false));
			}
		}
		
		return result;
	}
}
