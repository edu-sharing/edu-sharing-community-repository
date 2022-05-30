package org.edu_sharing.alfresco.policy;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.node.NodeServicePolicies.OnCreateNodePolicy;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.QName;
import org.edu_sharing.alfresco.workspace_administration.NodeServiceInterceptor;
import org.edu_sharing.repository.client.tools.CCConstants;

public class OnCreateInheritPermFalsePolicy implements OnCreateNodePolicy {

	PermissionService permissionService;
	
	PolicyComponent policyComponent;

	private String scope;
	
	public void initForFolders() {
		policyComponent.bindClassBehaviour(OnCreateNodePolicy.QNAME, QName.createQName(CCConstants.CCM_TYPE_MAP), new JavaBehaviour(this, "onCreateNode"));
		policyComponent.bindClassBehaviour(OnCreateNodePolicy.QNAME, ContentModel.TYPE_FOLDER, new JavaBehaviour(this, "onCreateNode"));
	}
	public void initForFoldersSafe() {
		initForFolders();
		scope=CCConstants.CCM_VALUE_SCOPE_SAFE;
	}
	public void initForFoldersAndContentSafe() {
		initForFoldersAndContent();
		scope=CCConstants.CCM_VALUE_SCOPE_SAFE;
	}
	
	public void initForFoldersAndContent() {
		initForFolders();
		
		policyComponent.bindClassBehaviour(OnCreateNodePolicy.QNAME, QName.createQName(CCConstants.CCM_TYPE_IO), new JavaBehaviour(this, "onCreateNode"));
		policyComponent.bindClassBehaviour(OnCreateNodePolicy.QNAME, ContentModel.TYPE_CONTENT, new JavaBehaviour(this, "onCreateNode"));
	}
	
	@Override
	public void onCreateNode(ChildAssociationRef childAssocRef) {
		// if scope active, only init the behaviour when in scoped context
		if(scope!=null && !scope.equals(NodeServiceInterceptor.getEduSharingScope()))
			return;
		
		NodeRef eduNodeRef = childAssocRef.getChildRef();
		permissionService.setInheritParentPermissions(eduNodeRef, false);
	
	}
	
	
	public void setPermissionService(PermissionService permissionService) {
		this.permissionService = permissionService;
	}
	
	public void setPolicyComponent(PolicyComponent policyComponent) {
		this.policyComponent = policyComponent;
	}
	
	
}
