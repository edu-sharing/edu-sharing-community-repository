package org.edu_sharing.alfresco.policy;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.node.NodeServicePolicies;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.edu_sharing.repository.client.tools.CCConstants;

/**
 * Generic guard class to check if the child is allowed to be placed in a specific folder
 * Especially guards all edu sys folders and e.g. prevents to be let any files to be placed in the toolpermission folders
 */
public class BeforeCreateNodePolicyGuardSysFolders implements NodeServicePolicies.BeforeCreateNodePolicy, NodeServicePolicies.BeforeMoveNodePolicy {

	PolicyComponent policyComponent;
	
	NodeService nodeService;
	
	public void init() {
		
		//OnCreateNode
		policyComponent.bindClassBehaviour(NodeServicePolicies.BeforeCreateNodePolicy.QNAME, ContentModel.TYPE_FOLDER, new JavaBehaviour(this, "beforeCreateNode"));
		policyComponent.bindClassBehaviour(NodeServicePolicies.BeforeCreateNodePolicy.QNAME, ContentModel.TYPE_CONTENT, new JavaBehaviour(this, "beforeCreateNode"));
		policyComponent.bindClassBehaviour(NodeServicePolicies.BeforeCreateNodePolicy.QNAME, QName.createQName(CCConstants.CCM_TYPE_IO), new JavaBehaviour(this, "beforeCreateNode"));
		policyComponent.bindClassBehaviour(NodeServicePolicies.BeforeCreateNodePolicy.QNAME, QName.createQName(CCConstants.CCM_TYPE_MAP), new JavaBehaviour(this, "beforeCreateNode"));
		
		//OnMoveNode
		policyComponent.bindClassBehaviour(NodeServicePolicies.BeforeMoveNodePolicy.QNAME, ContentModel.TYPE_FOLDER, new JavaBehaviour(this, "beforeMoveNode"));
		policyComponent.bindClassBehaviour(NodeServicePolicies.BeforeMoveNodePolicy.QNAME, ContentModel.TYPE_CONTENT, new JavaBehaviour(this, "beforeMoveNode"));
		policyComponent.bindClassBehaviour(NodeServicePolicies.BeforeMoveNodePolicy.QNAME, QName.createQName(CCConstants.CCM_TYPE_IO), new JavaBehaviour(this, "beforeMoveNode"));
		policyComponent.bindClassBehaviour(NodeServicePolicies.BeforeMoveNodePolicy.QNAME, QName.createQName(CCConstants.CCM_TYPE_MAP), new JavaBehaviour(this, "beforeMoveNode"));
	}

	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}

	public void setPolicyComponent(PolicyComponent policyComponent) {
		this.policyComponent = policyComponent;
	}

	@Override
	public void beforeCreateNode(NodeRef parentRef, QName assocTypeQName, QName assocQName, QName nodeTypeQName) {
		throwIfNodeIsNotAllowed(parentRef,nodeTypeQName);
	}

	@Override
	public void beforeMoveNode(ChildAssociationRef childAssociationRef, NodeRef newParentRef) {
		throwIfNodeIsNotAllowed(newParentRef,nodeService.getType(childAssociationRef.getChildRef()));
	}

	private void throwIfNodeIsNotAllowed(NodeRef parentRef, QName nodeType) {
		String mapType = (String) nodeService.getProperty(parentRef, QName.createQName(CCConstants.CCM_PROP_MAP_TYPE));
		if(mapType!=null && !mapType.isEmpty()){

			if(mapType.equals(CCConstants.CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM_TOOLPERMISSIONS)){
				if(!nodeType.equals(QName.createQName(CCConstants.CCM_TYPE_TOOLPERMISSION))) {
					throw new NodeCreateDeniedException("In folder of type " + CCConstants.CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM_TOOLPERMISSIONS + " only nodes of type " + CCConstants.CCM_TYPE_TOOLPERMISSION + " are allowed");
				}
			}

		}

	}
}
