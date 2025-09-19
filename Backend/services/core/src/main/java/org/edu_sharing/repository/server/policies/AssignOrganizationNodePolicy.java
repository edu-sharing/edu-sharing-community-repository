package org.edu_sharing.repository.server.policies;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.repo.node.NodeServicePolicies;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.edu_sharing.repository.client.rpc.EduGroup;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.restservices.RepositoryDao;
import org.edu_sharing.service.organization.OrganizationService;
import org.edu_sharing.service.search.SearchService;
import org.edu_sharing.service.search.SearchServiceFactory;
import org.edu_sharing.service.search.model.SearchResult;
import org.edu_sharing.spring.conditions.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "repository.organizations", name = "markOwnership", havingValue = "true")
public class AssignOrganizationNodePolicy implements NodeServicePolicies.OnCreateNodePolicy {

    private final PolicyComponent policyComponent;
    //    private final OrganizationService organizationService;
    private final NodeService nodeService;
    private SearchService searchService;

    public AssignOrganizationNodePolicy(PolicyComponent policyComponent, NodeService nodeService) {
        this.policyComponent = policyComponent;
        this.nodeService = nodeService;
    }

    @PostConstruct
    public void init() {
        this.searchService = SearchServiceFactory.getLocalService();
        policyComponent.bindClassBehaviour(NodeServicePolicies.OnCreateNodePolicy.QNAME, QName.createQName(CCConstants.CCM_TYPE_IO), new JavaBehaviour(this, "onCreateNode"));
    }

    @Override
    public void onCreateNode(ChildAssociationRef childAssocRef) {

        try {

            SearchResult<EduGroup> eduGroupSearchResult = searchService.searchOrganizations(null, 0, 100, null, false, true);
            List<String> orgaList = eduGroupSearchResult.getData().stream().map(EduGroup::getGroupId).toList();
            if (orgaList.isEmpty()) {
                return;
            }

            nodeService.addProperties(childAssocRef.getChildRef(), Map.of(QName.createQName(CCConstants.CCM_PROP_OWNING_EDUGROUP), (Serializable) orgaList));
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
        }

    }
}
