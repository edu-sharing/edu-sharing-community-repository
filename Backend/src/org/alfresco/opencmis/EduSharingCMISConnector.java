package org.alfresco.opencmis;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;

import net.sf.acegisecurity.AuthenticationCredentialsNotFoundException;

public class EduSharingCMISConnector extends CMISConnector {
	
	private ServiceRegistry serviceRegistry;
	
	@Override
	public NodeRef getRootNodeRef() {
		try{
			NodeRef nodeRef = this.serviceRegistry.getPersonService().getPerson(AuthenticationUtil.getFullyAuthenticatedUser(), false);
			NodeRef homeFolder = (NodeRef)this.serviceRegistry.getNodeService().getProperty(nodeRef, ContentModel.PROP_HOMEFOLDER);
			if(homeFolder != null){
				return homeFolder;
			}
		}catch(AuthenticationCredentialsNotFoundException e){
			e.printStackTrace();
		}
		
		return super.getRootNodeRef();
		
			
	}
	
	@Override
	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry; 
		super.setServiceRegistry(serviceRegistry);
	}
}
