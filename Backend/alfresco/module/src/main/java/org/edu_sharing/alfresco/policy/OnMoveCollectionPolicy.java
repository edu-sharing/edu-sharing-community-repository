package org.edu_sharing.alfresco.policy;

import org.alfresco.repo.node.NodeServicePolicies;
import org.alfresco.repo.node.NodeServicePolicies.OnMoveNodePolicy;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.edu_sharing.repository.client.tools.CCConstants;

/**
 * sets collection level0 to false when a root collection is moved to a subcollection
 */
public class OnMoveCollectionPolicy implements OnMoveNodePolicy {

    NodeService nodeService;

    PolicyComponent policyComponent;

    QName aspectCollection = QName.createQName(CCConstants.CCM_ASPECT_COLLECTION);
    QName propertyLevel0 = QName.createQName(CCConstants.CCM_PROP_MAP_COLLECTIONLEVEL0);

    public void init() {
        policyComponent.bindClassBehaviour(NodeServicePolicies.OnMoveNodePolicy.QNAME, QName.createQName(CCConstants.CCM_TYPE_MAP), new JavaBehaviour(this, "onMoveNode"));
    }

    @Override
    public void onMoveNode(ChildAssociationRef oldChildAssocRef, ChildAssociationRef newChildAssocRef) {
        NodeRef toMove = oldChildAssocRef.getChildRef();
        if (nodeService.hasAspect(toMove, aspectCollection)
                && (boolean) nodeService.getProperty(toMove, propertyLevel0) == true
                && nodeService.hasAspect(newChildAssocRef.getParentRef(), aspectCollection)) {
            nodeService.setProperty(toMove, propertyLevel0, false);
        }
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setPolicyComponent(PolicyComponent policyComponent) {
        this.policyComponent = policyComponent;
    }
}
