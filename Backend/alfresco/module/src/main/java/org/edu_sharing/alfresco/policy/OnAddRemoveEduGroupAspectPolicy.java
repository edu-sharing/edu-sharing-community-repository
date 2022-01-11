package org.edu_sharing.alfresco.policy;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.node.NodeServicePolicies.OnAddAspectPolicy;
import org.alfresco.repo.node.NodeServicePolicies.OnRemoveAspectPolicy;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.cache.EduGroupCache;

public class OnAddRemoveEduGroupAspectPolicy implements OnAddAspectPolicy, OnRemoveAspectPolicy {
	
	NodeService nodeService;
	PolicyComponent policyComponent;
	
	Logger logger = Logger.getLogger(OnAddRemoveEduGroupAspectPolicy.class);
	
	public void init(){
		//policyComponent.bindClassBehaviour(OnAddAspectPolicy.QNAME, ContentModel.TYPE_AUTHORITY_CONTAINER, new JavaBehaviour(this, "onAddAspect"));
		//policyComponent.bindClassBehaviour(OnRemoveAspectPolicy.QNAME, ContentModel.TYPE_AUTHORITY_CONTAINER, new JavaBehaviour(this, "onRemoveAspect"));
		
		policyComponent.bindClassBehaviour(OnAddAspectPolicy.QNAME, QName.createQName(CCConstants.CCM_ASPECT_EDUGROUP ), new JavaBehaviour(this, "onAddAspect"));
		policyComponent.bindClassBehaviour(OnRemoveAspectPolicy.QNAME, QName.createQName(CCConstants.CCM_ASPECT_EDUGROUP ), new JavaBehaviour(this, "onRemoveAspect"));
		
	}
	
	@Override
	public void onAddAspect(NodeRef nodeRef, QName aspectQName) {
		if(aspectQName.equals(QName.createQName(CCConstants.CCM_ASPECT_EDUGROUP ))){
			EduGroupCache.put(nodeRef, nodeService.getProperties(nodeRef));
		}
		
	}
	
	@Override
	public void onRemoveAspect(NodeRef nodeRef, QName aspectQName) {
		if(aspectQName.equals(QName.createQName(CCConstants.CCM_ASPECT_EDUGROUP ))){
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
