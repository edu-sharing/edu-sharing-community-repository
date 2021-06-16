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

public class OnCopyMapPolicy implements OnCopyCompletePolicy{
	
	PolicyComponent policyComponent;
	
	NodeService nodeService;
	
	Logger logger = Logger.getLogger(OnCopyMapPolicy.class);
	
	QName versionProp = QName.createQName(CCConstants.LOM_PROP_LIFECYCLE_VERSION);
	
	public void init(){
		this.policyComponent.bindClassBehaviour(QName.createQName(NamespaceService.ALFRESCO_URI, "onCopyComplete"),
               QName.createQName(CCConstants.CCM_TYPE_MAP), new JavaBehaviour(this, "onCopyComplete"));
	}
	
	@Override
	public void onCopyComplete(QName classRef, NodeRef sourceNodeRef, NodeRef targetNodeRef, boolean copyToNewNode, Map<NodeRef, NodeRef> copyMap) {
		nodeService.removeProperty(targetNodeRef, QName.createQName(CCConstants.CCM_PROP_MAP_TYPE));
		
		//remove old permissionhistory, current entry will be added by edu-sharing NodeDao
		if(nodeService.hasAspect(targetNodeRef, QName.createQName(CCConstants.CCM_ASPECT_PERMISSION_HISTORY))) {
			nodeService.removeProperty(targetNodeRef, QName.createQName(CCConstants.CCM_PROP_PH_HISTORY));
			nodeService.removeProperty(targetNodeRef, QName.createQName(CCConstants.CCM_PROP_PH_INVITED));
			nodeService.removeProperty(targetNodeRef, QName.createQName(CCConstants.CCM_PROP_PH_ACTION));
			nodeService.removeProperty(targetNodeRef, QName.createQName(CCConstants.CCM_PROP_PH_MODIFIED));
			nodeService.removeProperty(targetNodeRef, QName.createQName(CCConstants.CCM_PROP_PH_USERS));
		}
	}
	
	public void setPolicyComponent(PolicyComponent policyComponent) {
		this.policyComponent = policyComponent;
	}
	
	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}
}
