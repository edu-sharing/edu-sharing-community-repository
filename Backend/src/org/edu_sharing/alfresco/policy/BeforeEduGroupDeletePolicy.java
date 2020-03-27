package org.edu_sharing.alfresco.policy;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.node.NodeServicePolicies;
import org.alfresco.repo.node.NodeServicePolicies.BeforeDeleteNodePolicy;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.cache.EduGroupCache;

public class BeforeEduGroupDeletePolicy implements NodeServicePolicies.BeforeDeleteNodePolicy {

	
	NodeService nodeService;
	PolicyComponent policyComponent;
	AuthenticationService authenticationService;
	AuthorityService authorityService;
	
	Logger logger = Logger.getLogger(BeforeEduGroupDeletePolicy.class);
	
	public void init(){
		policyComponent.bindClassBehaviour(BeforeDeleteNodePolicy.QNAME, ContentModel.TYPE_AUTHORITY_CONTAINER, new JavaBehaviour(this, "beforeDeleteNode"));
	}
	
	@Override
	public void beforeDeleteNode(NodeRef nodeRef) {
		if(nodeService.hasAspect(nodeRef, QName.createQName(CCConstants.CCM_ASPECT_EDUGROUP ))){
			if( !new Helper(authorityService).isAdmin(authenticationService.getCurrentUserName()) 
					&& !AuthenticationUtil.isRunAsUserTheSystemUser() ){
				throw new SystemFolderDeleteDeniedException("you are not allowed to remove this folder!");
			}
			EduGroupCache.remove(nodeRef);
		}
		
	}
	
	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}
	
	public void setPolicyComponent(PolicyComponent policyComponent) {
		this.policyComponent = policyComponent;
	}
	
	public void setAuthenticationService(AuthenticationService authenticationService) {
		this.authenticationService = authenticationService;
	}
	
	public void setAuthorityService(AuthorityService authorityService) {
		this.authorityService = authorityService;
	}
}
