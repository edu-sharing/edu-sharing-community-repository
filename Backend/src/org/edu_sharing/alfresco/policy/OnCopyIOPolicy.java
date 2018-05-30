package org.edu_sharing.alfresco.policy;

import java.io.Serializable;
import java.util.Map;

import org.alfresco.repo.copy.CopyBehaviourCallback;
import org.alfresco.repo.copy.CopyDetails;
import org.alfresco.repo.copy.CopyServicePolicies.OnCopyCompletePolicy;
import org.alfresco.repo.copy.DefaultCopyBehaviourCallback;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;

public class OnCopyIOPolicy implements OnCopyCompletePolicy{
	
	PolicyComponent policyComponent;
	
	NodeService nodeService;
	
	Logger logger = Logger.getLogger(OnCopyIOPolicy.class);
	
	QName versionProp = QName.createQName(CCConstants.LOM_PROP_LIFECYCLE_VERSION);
	
	public void init(){
		this.policyComponent.bindClassBehaviour(QName.createQName(NamespaceService.ALFRESCO_URI, "onCopyComplete"),
               QName.createQName(CCConstants.CCM_TYPE_IO), new JavaBehaviour(this, "onCopyComplete"));
	}
	
	
	
	@Override
	public void onCopyComplete(QName classRef, NodeRef sourceNodeRef, NodeRef targetNodeRef, boolean copyToNewNode, Map<NodeRef, NodeRef> copyMap) {
		logger.info("will set " + versionProp.toPrefixString() + " to 1.0");
		nodeService.setProperty(targetNodeRef, versionProp, "1.0");
		
		QName publishedAspect = QName.createQName(CCConstants.CCM_ASPECT_PUBLISHED);
		if(nodeService.hasAspect(targetNodeRef, publishedAspect)) {
			nodeService.removeAspect(targetNodeRef, publishedAspect);
		}
	}
	
	
	public void setPolicyComponent(PolicyComponent policyComponent) {
		this.policyComponent = policyComponent;
	}
	
	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}
}
