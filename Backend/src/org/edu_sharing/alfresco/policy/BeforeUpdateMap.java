package org.edu_sharing.alfresco.policy;

import org.alfresco.repo.node.NodeServicePolicies;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.permissions.AccessDeniedException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.namespace.QName;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.cache.EduGroupCache;

import java.util.List;

public class BeforeUpdateMap implements NodeServicePolicies.BeforeUpdateNodePolicy {

    PolicyComponent policyComponent;
    NodeService nodeService;
    AuthorityService authorityService;

    public void init(){
        policyComponent.bindClassBehaviour(NodeServicePolicies.BeforeUpdateNodePolicy.QNAME, QName.createQName(CCConstants.CCM_TYPE_MAP), new JavaBehaviour(this, "beforeUpdateNode"));
    }

    @Override
    public void beforeUpdateNode(NodeRef nodeRef) {
        List<NodeRef> allEduGoupFolder = EduGroupCache.getAllEduGoupFolder();
        if(allEduGoupFolder == null) return;
        if(allEduGoupFolder.contains(nodeRef)){
            if(!AuthenticationUtil.isRunAsUserTheSystemUser()
                    && !authorityService.isAdminAuthority(AuthenticationUtil.getRunAsUser())){
                throw new AccessDeniedException("Organisation Folder should not be modified!");
            }
        }
    }


    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setPolicyComponent(PolicyComponent policyComponent) {
        this.policyComponent = policyComponent;
    }

    public void setAuthorityService(AuthorityService authorityService) {
        this.authorityService = authorityService;
    }
}
