package org.edu_sharing.alfresco.policy;

import org.alfresco.model.ContentModel;
import org.alfresco.model.ImapModel;
import org.alfresco.repo.node.NodeServicePolicies;
import org.alfresco.repo.policy.Behaviour;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AccessPermission;
import org.alfresco.service.cmr.security.OwnableService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;

import java.util.Set;

/**
 * set owner of parent object to coordinator so that this permission inherits to child object (cild io or comments)
 *
 */
public class OnAddIOChildObjectAspect implements NodeServicePolicies.OnAddAspectPolicy, NodeServicePolicies.OnCreateChildAssociationPolicy {

    PolicyComponent policyComponent;
    NodeService nodeService;
    PermissionService permissionService;
    OwnableService ownableService;

    Logger logger = Logger.getLogger(OnAddIOChildObjectAspect.class);

    public void init(){
        policyComponent.bindClassBehaviour(NodeServicePolicies.OnAddAspectPolicy.QNAME,
                QName.createQName(CCConstants.CCM_ASPECT_IO_CHILDOBJECT ),
                new JavaBehaviour(this, "onAddAspect"));

        policyComponent.bindAssociationBehaviour(NodeServicePolicies.OnCreateChildAssociationPolicy.QNAME,
                QName.createQName(CCConstants.CCM_TYPE_IO), QName.createQName(CCConstants.CCM_ASSOC_COMMENT),
                new JavaBehaviour(this, "onCreateChildAssociation"));

    }

    /**
     *
     * @param nodeRef
     * @param aspectTypeQName
     */
    @Override
    public void onAddAspect(NodeRef nodeRef, QName aspectTypeQName) {

        ChildAssociationRef parentAssoc = nodeService.getPrimaryParent(nodeRef);
        if(QName.createQName(CCConstants.CCM_TYPE_IO).equals(nodeService.getType(parentAssoc.getParentRef()))){
            ownerAsCoordinator(parentAssoc.getParentRef());
        }else{
            logger.error("parent is no ccm:io");
        }
    }

    /**
     *
     * @param childAssociationRef
     * @param b
     */
    @Override
    public void onCreateChildAssociation(ChildAssociationRef childAssociationRef, boolean b) {
        ownerAsCoordinator(childAssociationRef.getParentRef());
    }

    private void ownerAsCoordinator(NodeRef nodeRef){
        AuthenticationUtil.runAsSystem(()->{
            String owner = ownableService.getOwner(nodeRef);
            Set<AccessPermission> allSetPermissions = permissionService.getAllSetPermissions(nodeRef);
            for(AccessPermission ap : allSetPermissions){
                if(ap.getAuthority().equals(owner) && PermissionService.COORDINATOR.equals(ap.getPermission())){
                    return null;
                }
            }
            permissionService.setPermission(nodeRef, owner, PermissionService.COORDINATOR, true);
            return null;
        });
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
