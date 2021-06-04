package org.edu_sharing.repository.server.jobs.quartz;

import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AccessPermission;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;

public class TakeBackGlobalShareWorker {
	
	NodeService nodeService;
	PermissionService permissionService;
	boolean execute;
	
	Logger logger = Logger.getLogger(TakeBackGlobalShareWorker.class);
	
	public TakeBackGlobalShareWorker(NodeService nodeService, PermissionService permissionService, boolean execute) {
		this.nodeService = nodeService;
		this.permissionService = permissionService;
		this.execute = execute;
	}
	
	public void work(NodeRef nodeRef) {
		String name = (String)nodeService.getProperty(nodeRef, ContentModel.PROP_NAME);
		QName type = (QName)nodeService.getType(nodeRef);
		
		if(type.equals(QName.createQName(CCConstants.CCM_TYPE_TOOLPERMISSION))) {
			return;
		}
		
		//leave out schoolcontext objects
		if(!(type.equals(QName.createQName(CCConstants.CCM_TYPE_IO)) 
				|| type.equals(QName.createQName(CCConstants.CCM_TYPE_MAP))
				|| type.equals(QName.createQName(CCConstants.CM_TYPE_FOLDER))
				|| type.equals(QName.createQName(CCConstants.CCM_TYPE_NOTIFY)))) {
			return;
		}
		
		Set<AccessPermission> setPermissions = permissionService.getAllSetPermissions(nodeRef);
		for(AccessPermission ap : setPermissions) {
			String authority = ap.getAuthority();
			String permission = ap.getPermission();
			boolean isInherited = ap.isInherited();
			
			if(PermissionService.ALL_AUTHORITIES.equals(authority) 
					&& !isInherited) {
				logger.info("removing permission"  + " nodeRef:" + nodeRef  + ";name:" + name + ";type" + type + ";permission:" + permission);
				if(execute) {
					permissionService.deletePermission(nodeRef, authority, permission);
				}
			}
		}
	}

}
