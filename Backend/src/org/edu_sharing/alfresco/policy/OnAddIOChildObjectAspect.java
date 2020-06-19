package org.edu_sharing.alfresco.policy;

import org.alfresco.repo.node.NodeServicePolicies;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.OwnableService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;


public class OnAddIOChildObjectAspect implements NodeServicePolicies.OnAddAspectPolicy {

    PolicyComponent policyComponent;
    NodeService nodeService;
    PermissionService permissionService;
    OwnableService ownableService;

    Logger logger = Logger.getLogger(OnAddIOChildObjectAspect.class);

    public void init(){
        policyComponent.bindClassBehaviour(NodeServicePolicies.OnAddAspectPolicy.QNAME,
                QName.createQName(CCConstants.CCM_ASPECT_IO_CHILDOBJECT ),
                new JavaBehaviour(this, "onAddAspect"));
    }

    /**
     * set owner of parent object to coordinator so that this permission inherits to child_io
     * @param nodeRef
     * @param aspectTypeQName
     */
    @Override
    public void onAddAspect(NodeRef nodeRef, QName aspectTypeQName) {

        ChildAssociationRef parentAssoc = nodeService.getPrimaryParent(nodeRef);
        if(QName.createQName(CCConstants.CCM_TYPE_IO).equals(nodeService.getType(parentAssoc.getParentRef()))){
            String owner = ownableService.getOwner(parentAssoc.getParentRef());
            permissionService.setPermission(parentAssoc.getParentRef(), owner, PermissionService.COORDINATOR, true);
        }else{
            logger.error("parent is no ccm:io");
        }
    }

    public void setPolicyComponent(PolicyComponent policyComponent) {
        this.policyComponent = policyComponent;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setPermissionService(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    public void setOwnableService(OwnableService ownableService) {
        this.ownableService = ownableService;
    }
}
