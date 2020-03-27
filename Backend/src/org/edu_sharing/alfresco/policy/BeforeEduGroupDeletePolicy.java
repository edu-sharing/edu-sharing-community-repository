package org.edu_sharing.alfresco.policy;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.node.NodeServicePolicies;
import org.alfresco.repo.node.NodeServicePolicies.BeforeDeleteNodePolicy;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.cache.EduGroupCache;

public class BeforeEduGroupDeletePolicy implements NodeServicePolicies.BeforeDeleteNodePolicy {

	
	NodeService nodeService;
	PolicyComponent policyComponent;
	
	Logger logger = Logger.getLogger(BeforeEduGroupDeletePolicy.class);
	
	public void init(){
		policyComponent.bindClassBehaviour(BeforeDeleteNodePolicy.QNAME, ContentModel.TYPE_AUTHORITY_CONTAINER, new JavaBehaviour(this, "beforeDeleteNode"));
	}
	
	@Override
	public void beforeDeleteNode(NodeRef nodeRef) {
		if(nodeService.hasAspect(nodeRef, QName.createQName(CCConstants.CCM_ASPECT_EDUGROUP ))){
			
			EduGroupCache.remove(nodeRef);
		}
		
	}
	
	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}
	
	public void setPolicyComponent(PolicyComponent policyComponent) {
		this.policyComponent = policyComponent;
	}
}
