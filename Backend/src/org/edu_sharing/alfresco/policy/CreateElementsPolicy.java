package org.edu_sharing.alfresco.policy;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.node.NodeServicePolicies.OnCreateNodePolicy;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.edu_sharing.alfresco.service.toolpermission.ToolPermissionBaseService;
import org.edu_sharing.alfresco.service.toolpermission.ToolPermissionException;
import org.edu_sharing.repository.client.tools.CCConstants;

public class CreateElementsPolicy implements OnCreateNodePolicy {

	PolicyComponent policyComponent;
	
	NodeService nodeService;
	
	public void init() {
		
		//OnCreateNode
		policyComponent.bindClassBehaviour(OnCreateNodePolicy.QNAME, QName.createQName(CCConstants.CCM_TYPE_MAP), new JavaBehaviour(this, "onCreateNode"));
		policyComponent.bindClassBehaviour(OnCreateNodePolicy.QNAME, ContentModel.TYPE_FOLDER, new JavaBehaviour(this, "onCreateNode"));
		policyComponent.bindClassBehaviour(OnCreateNodePolicy.QNAME, QName.createQName(CCConstants.CCM_TYPE_IO), new JavaBehaviour(this, "onCreateNode"));
		policyComponent.bindClassBehaviour(OnCreateNodePolicy.QNAME, ContentModel.TYPE_CONTENT, new JavaBehaviour(this, "onCreateNode"));
	}

	@Override
	public void onCreateNode(ChildAssociationRef childRef) {
		NodeRef nodeRef = childRef.getChildRef();
		if(nodeService.hasAspect(nodeRef, QName.createQName(CCConstants.CCM_ASPECT_COLLECTION))){
			throwIfToolpermissionMissing(CCConstants.CCM_VALUE_TOOLPERMISSION_CREATE_ELEMENTS_COLLECTIONS);
		} else if(nodeService.getType(nodeRef).equals(QName.createQName(CCConstants.CCM_TYPE_MAP))){
			throwIfToolpermissionMissing(CCConstants.CCM_VALUE_TOOLPERMISSION_CREATE_ELEMENTS_FOLDERS);
		} else {
			throwIfToolpermissionMissing(CCConstants.CCM_VALUE_TOOLPERMISSION_CREATE_ELEMENTS_FILES);
		}
	}
	private void throwIfToolpermissionMissing(String toolpermission){
		if(!new ToolPermissionBaseService().hasToolPermission(toolpermission)){
			throw new ToolPermissionException(toolpermission);
		}
	}

	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}
	
	public void setPolicyComponent(PolicyComponent policyComponent) {
		this.policyComponent = policyComponent;
	}
}
