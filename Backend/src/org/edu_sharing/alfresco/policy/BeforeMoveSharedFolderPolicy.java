package org.edu_sharing.alfresco.policy;

import org.alfresco.repo.node.NodeServicePolicies;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.namespace.QName;
import org.edu_sharing.repository.client.tools.CCConstants;

public class BeforeMoveSharedFolderPolicy implements NodeServicePolicies.OnMoveNodePolicy{

	
	NodeService nodeService;
	PolicyComponent policyComponent;
	AuthorityService authorityService;
	AuthenticationService authenticationService;
	
	public void init(){
		policyComponent.bindClassBehaviour(NodeServicePolicies.OnMoveNodePolicy.QNAME, QName.createQName(CCConstants.CCM_TYPE_MAP), new JavaBehaviour(this, "onMoveNode"));
	}
	
	@Override
	public void onMoveNode(ChildAssociationRef oldChildAssocRef, ChildAssociationRef newChildAssocRef) {
		
		if(nodeService.getType(oldChildAssocRef.getChildRef()).equals(QName.createQName(CCConstants.CCM_TYPE_MAP))){
			String mapType = (String)nodeService.getProperty(oldChildAssocRef.getChildRef(), QName.createQName(CCConstants.CCM_PROP_MAP_TYPE) );
			if(mapType != null && mapType.trim().equals(CCConstants.CCM_VALUE_MAP_TYPE_EDUGROUP) && !new Helper(authorityService).isAdmin(authenticationService.getCurrentUserName())){
				throw new RuntimeException("Move of this folder is not allowed");
			}
		}
		
		
	}
	
	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}
	
	public void setPolicyComponent(PolicyComponent policyComponent) {
		this.policyComponent = policyComponent;
	}
	
	public void setAuthenticationService(
			AuthenticationService authenticationService) {
		this.authenticationService = authenticationService;
	}
	
	public void setAuthorityService(AuthorityService authorityService) {
		this.authorityService = authorityService;
	}
	
}
