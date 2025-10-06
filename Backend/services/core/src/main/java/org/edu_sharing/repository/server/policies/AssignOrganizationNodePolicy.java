package org.edu_sharing.repository.server.policies;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.repo.node.NodeServicePolicies;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.organization.OrganizationService;
import org.edu_sharing.spring.conditions.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "repository.organizations", name = "markOwnership", havingValue = "true")
public class AssignOrganizationNodePolicy implements NodeServicePolicies.OnCreateNodePolicy {

    private final PolicyComponent policyComponent;
    private final OrganizationService organizationService;
    private final NodeService nodeService;

    @PostConstruct
    public void init() {
        policyComponent.bindClassBehaviour(NodeServicePolicies.OnCreateNodePolicy.QNAME, QName.createQName(CCConstants.CCM_TYPE_IO), new JavaBehaviour(this, "onCreateNode"));
    }

    @Override
    public void onCreateNode(ChildAssociationRef childAssocRef) {

        try {
            String currentUser = AuthenticationUtil.getFullyAuthenticatedUser();
            Set<String> organisations = organizationService.getAssignedOrganisations(currentUser);
            if (organisations.isEmpty()) {
                return;
            }

            nodeService.addProperties(childAssocRef.getChildRef(), Map.of(QName.createQName(CCConstants.CCM_PROP_OWNING_EDUGROUP), (Serializable) organisations.stream().toList()));
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
        }

    }
}
