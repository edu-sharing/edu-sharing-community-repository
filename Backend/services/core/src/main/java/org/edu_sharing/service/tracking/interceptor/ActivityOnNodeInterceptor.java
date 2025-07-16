package org.edu_sharing.service.tracking.interceptor;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.alfresco.repo.content.ContentServicePolicies;
import org.alfresco.repo.copy.CopyServicePolicies;
import org.alfresco.repo.node.NodeServicePolicies;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.version.VersionServicePolicies;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.namespace.QName;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.tracking.ActivityEventService;
import org.edu_sharing.service.tracking.ActivityOnNodeEventType;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ActivityOnNodeInterceptor implements NodeServicePolicies.OnUpdatePropertiesPolicy,
        NodeServicePolicies.OnCreateNodePolicy,
        NodeServicePolicies.OnDeleteNodePolicy,
        NodeServicePolicies.OnMoveNodePolicy,
        CopyServicePolicies.OnCopyCompletePolicy,
        ContentServicePolicies.OnContentUpdatePolicy,
        VersionServicePolicies.AfterVersionRevertPolicy
{

    private final PolicyComponent policyComponent;
    private final ActivityEventService activityEventService;

    @PostConstruct
    public void init() {
        // onUpdateProperties
        policyComponent.bindClassBehaviour(NodeServicePolicies.OnUpdatePropertiesPolicy.QNAME, QName.createQName(CCConstants.CCM_TYPE_IO), new JavaBehaviour(this, "onUpdateProperties"));
        policyComponent.bindClassBehaviour(NodeServicePolicies.OnUpdatePropertiesPolicy.QNAME, QName.createQName(CCConstants.CCM_TYPE_MAP), new JavaBehaviour(this, "onUpdateProperties"));

        // onCreateNode
        policyComponent.bindClassBehaviour(NodeServicePolicies.OnCreateNodePolicy.QNAME, QName.createQName(CCConstants.CCM_TYPE_IO), new JavaBehaviour(this, "onCreateNode"));
        policyComponent.bindClassBehaviour(NodeServicePolicies.OnCreateNodePolicy.QNAME, QName.createQName(CCConstants.CCM_TYPE_MAP), new JavaBehaviour(this, "onCreateNode"));

        // onDeleteNode
        policyComponent.bindClassBehaviour(NodeServicePolicies.OnDeleteNodePolicy.QNAME, QName.createQName(CCConstants.CCM_TYPE_IO), new JavaBehaviour(this, "onDeleteNode"));
        policyComponent.bindClassBehaviour(NodeServicePolicies.OnDeleteNodePolicy.QNAME, QName.createQName(CCConstants.CCM_TYPE_MAP), new JavaBehaviour(this, "onDeleteNode"));

        //onMoveNode
        policyComponent.bindClassBehaviour(NodeServicePolicies.OnMoveNodePolicy.QNAME, QName.createQName(CCConstants.CCM_TYPE_IO), new JavaBehaviour(this, "onMoveNode"));
        policyComponent.bindClassBehaviour(NodeServicePolicies.OnMoveNodePolicy.QNAME, QName.createQName(CCConstants.CCM_TYPE_MAP), new JavaBehaviour(this, "onMoveNode"));


        //onCopyComplete
        policyComponent.bindClassBehaviour(CopyServicePolicies.OnCopyCompletePolicy.QNAME, QName.createQName(CCConstants.CCM_TYPE_IO), new JavaBehaviour(this, "onCopyComplete"));
        policyComponent.bindClassBehaviour(CopyServicePolicies.OnCopyCompletePolicy.QNAME, QName.createQName(CCConstants.CCM_TYPE_MAP), new JavaBehaviour(this, "onCopyComplete"));

        //onContentUpdate
        policyComponent.bindClassBehaviour(ContentServicePolicies.OnContentUpdatePolicy.QNAME, QName.createQName(CCConstants.CCM_TYPE_IO), new JavaBehaviour(this, "onContentUpdate"));
        policyComponent.bindClassBehaviour(ContentServicePolicies.OnContentUpdatePolicy.QNAME, QName.createQName(CCConstants.CCM_TYPE_MAP), new JavaBehaviour(this, "onContentUpdate"));

        //afterVersionRevert
        policyComponent.bindClassBehaviour(VersionServicePolicies.AfterVersionRevertPolicy.QNAME, QName.createQName(CCConstants.CCM_TYPE_IO), new JavaBehaviour(this, "afterVersionRevert"));
        policyComponent.bindClassBehaviour(VersionServicePolicies.AfterVersionRevertPolicy.QNAME, QName.createQName(CCConstants.CCM_TYPE_MAP), new JavaBehaviour(this, "afterVersionRevert"));
    }

    @Override
    public void onUpdateProperties(NodeRef nodeRef, Map<QName, Serializable> before, Map<QName, Serializable> after) {
        activityEventService.trackActivityOnNode(nodeRef, null, ActivityOnNodeEventType.EDIT_MATERIAL, AuthenticationUtil.getFullyAuthenticatedUser());
    }

    @Override
    public void onCreateNode(ChildAssociationRef childAssocRef) {
        activityEventService.trackActivityOnNode(childAssocRef.getChildRef(), null, ActivityOnNodeEventType.CREATE_MATERIAL, AuthenticationUtil.getFullyAuthenticatedUser());
    }

    @Override
    public void onDeleteNode(ChildAssociationRef childAssocRef, boolean isNodeArchived) {
        if (isNodeArchived) {
            activityEventService.trackActivityOnNode(childAssocRef.getChildRef(), null, ActivityOnNodeEventType.ARCHIVE_MATERIAL, AuthenticationUtil.getFullyAuthenticatedUser());
        } else {
            activityEventService.trackActivityOnNode(childAssocRef.getChildRef(), null, ActivityOnNodeEventType.DELETE_MATERIAL, AuthenticationUtil.getFullyAuthenticatedUser());
        }
    }

    @Override
    public void onMoveNode(ChildAssociationRef oldChildAssocRef, ChildAssociationRef newChildAssocRef) {
        activityEventService.trackActivityOnNode(newChildAssocRef.getChildRef(), null, ActivityOnNodeEventType.MOVE_MATERIAL, AuthenticationUtil.getFullyAuthenticatedUser());
    }

    @Override
    public void onCopyComplete(QName classRef, NodeRef sourceNodeRef, NodeRef targetNodeRef, boolean copyToNewNode, Map<NodeRef, NodeRef> copyMap) {
        activityEventService.trackActivityOnNode(targetNodeRef, null, ActivityOnNodeEventType.COPY_MATERIAL, AuthenticationUtil.getFullyAuthenticatedUser());
    }

    @Override
    public void onContentUpdate(NodeRef nodeRef, boolean newContent) {
        activityEventService.trackActivityOnNode(nodeRef, null, ActivityOnNodeEventType.CHANGE_MATERIAL_CONTENT, AuthenticationUtil.getFullyAuthenticatedUser());
    }

    @Override
    public void afterVersionRevert(NodeRef nodeRef, Version version) {
        activityEventService.trackActivityOnNode(nodeRef, null, ActivityOnNodeEventType.REVERT_MATERIAL_VERSION, AuthenticationUtil.getFullyAuthenticatedUser());
    }
}
