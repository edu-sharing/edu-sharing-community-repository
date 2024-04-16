package org.edu_sharing.alfresco.policy;

import org.alfresco.repo.node.NodeServicePolicies.OnMoveNodePolicy;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;

import java.io.Serializable;

/**
 * updates the usages for a moved collection ref
 */
public class OnMoveCollectionRefPolicy implements OnMoveNodePolicy {
    Logger logger = Logger.getLogger(OnMoveCollectionRefPolicy.class);


    NodeService nodeService;

    PolicyComponent policyComponent;

    public void init() {
        policyComponent.bindClassBehaviour(OnMoveNodePolicy.QNAME, QName.createQName(CCConstants.CCM_TYPE_IO), new JavaBehaviour(this, "onMoveNode"));
    }

    @Override
    public void onMoveNode(ChildAssociationRef oldChildAssocRef, ChildAssociationRef newChildAssocRef) {
        NodeRef toMove = oldChildAssocRef.getChildRef();
        if (nodeService.hasAspect(toMove, QName.createQName(CCConstants.CCM_ASPECT_COLLECTION_IO_REFERENCE))) {
            try {
                Serializable original = (Serializable) nodeService.getProperty(toMove, QName.createQName(CCConstants.CCM_PROP_IO_ORIGINAL));
                NodeRef originalRef;
                if(original instanceof String) {
                    originalRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, (String) original);
                } else {
                    originalRef = (NodeRef) original;
                }

                nodeService.getChildAssocs(originalRef).
                        stream().filter(
                                // type must be usage
                                usage -> QName.createQName(CCConstants.CCM_TYPE_USAGE).equals(nodeService.getType(usage.getChildRef()))
                        ).map(
                                ChildAssociationRef::getChildRef
                        ).filter(usage ->
                                // must have a pointer on the old collection/parent
                                nodeService.getProperty(usage, QName.createQName(CCConstants.CCM_PROP_USAGE_COURSEID)).equals(oldChildAssocRef.getParentRef().getId())
                        ).forEach(usage -> {
                            // set it to the new collection/parent
                            nodeService.setProperty(usage, QName.createQName(CCConstants.CCM_PROP_USAGE_COURSEID), newChildAssocRef.getParentRef().getId());
                        });
            } catch(Throwable t) {
                logger.info("Could not update collection refs original ");
            }
        }
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setPolicyComponent(PolicyComponent policyComponent) {
        this.policyComponent = policyComponent;
    }
}
