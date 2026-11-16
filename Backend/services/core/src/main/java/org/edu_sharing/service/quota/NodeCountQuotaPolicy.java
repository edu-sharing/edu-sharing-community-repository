package org.edu_sharing.service.quota;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.repo.node.NodeServicePolicies;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.repo.transaction.TransactionListenerAdapter;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.authority.AuthorityServiceHelper;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Enforces the per person count quotas (max. number of owned materials / collections / folders).
 *
 * Users without any count quota property are not affected at all.
 * The system user and global admins are never limited.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NodeCountQuotaPolicy implements NodeServicePolicies.OnCreateNodePolicy {

    private static final String KEY_CREATED_NODES = NodeCountQuotaPolicy.class.getName() + ".createdNodes";
    private static final String KEY_LIMITS = NodeCountQuotaPolicy.class.getName() + ".limits";

    private final PolicyComponent policyComponent;
    private final NodeService nodeService;

    @PostConstruct
    public void init() {
        for (String type : List.of(CCConstants.CCM_TYPE_IO, CCConstants.CCM_TYPE_MAP)) {
            policyComponent.bindClassBehaviour(NodeServicePolicies.OnCreateNodePolicy.QNAME, QName.createQName(type), new JavaBehaviour(this, "onCreateNode"));
        }
    }

    @Override
    public void onCreateNode(ChildAssociationRef childAssocRef) {
        // use the fully authenticated user (not the run as user), because many nodes are created inside runAsSystem
        // on behalf of the current user (e.g. collections, see CollectionServiceElastic.create)
        String user = AuthenticationUtil.getFullyAuthenticatedUser();
        if (user == null || AuthenticationUtil.getSystemUserName().equals(user)) {
            return;
        }
        if (getLimits(user).isEmpty()) {
            return;
        }
        Map<String, Set<NodeRef>> createdNodes = AlfrescoTransactionSupport.getResource(KEY_CREATED_NODES);
        if (createdNodes == null) {
            createdNodes = new HashMap<>();
            AlfrescoTransactionSupport.bindResource(KEY_CREATED_NODES, createdNodes);
            AlfrescoTransactionSupport.bindListener(new QuotaTransactionListener());
        }
        createdNodes.computeIfAbsent(user, k -> new LinkedHashSet<>()).add(childAssocRef.getChildRef());
    }

    /**
     * @return the limits of the user (empty for users without limits and for admins), cached per transaction
     */
    private Map<NodeCountQuotaType, Long> getLimits(String user) {
        Map<String, Map<NodeCountQuotaType, Long>> cache = AlfrescoTransactionSupport.getResource(KEY_LIMITS);
        if (cache == null) {
            cache = new HashMap<>();
            AlfrescoTransactionSupport.bindResource(KEY_LIMITS, cache);
        }
        return cache.computeIfAbsent(user, u -> {
            if(AuthorityServiceHelper.isAdmin(u)) {
                return Collections.emptyMap();
            }
            return NodeCountQuotaHelper.getLimits(u);
        });
    }

    private void checkQuotas(String user, Collection<NodeRef> nodes) {
        Map<NodeCountQuotaType, Long> limits = getLimits(user);
        for (Map.Entry<NodeCountQuotaType, Long> limit : limits.entrySet()) {
            NodeCountQuotaType type = limit.getKey();
            long created = AuthenticationUtil.runAsSystem(() -> nodes.stream()
                    .filter(nodeService::exists)
                    .filter(nodeRef -> type.matches(nodeService, nodeRef))
                    .count());
            if (created == 0) {
                continue;
            }
            Long existing;
            try {
                existing = NodeCountQuotaHelper.countOwnedNodes(user, type);
            } catch (Throwable t) {
                log.warn("Could not count the owned nodes of {} for the count quota {}, quota is not enforced: {}", user, type, t.getMessage());
                continue;
            }
            if (existing == null) {
                log.debug("No elastic search available, count quota {} is not enforced", type);
                continue;
            }
            long total = existing + created;
            if (total > limit.getValue()) {
                log.info("Count quota {} of user {} exceeded: {} / {}", type, user, total, limit.getValue());
                throw new NodeCountQuotaExceededException(type, limit.getValue(), existing);
            }
        }
    }

    private class QuotaTransactionListener extends TransactionListenerAdapter {
        @Override
        public void beforeCommit(boolean readOnly) {
            Map<String, Set<NodeRef>> createdNodes = AlfrescoTransactionSupport.getResource(KEY_CREATED_NODES);
            if (createdNodes == null) {
                return;
            }
            createdNodes.forEach(NodeCountQuotaPolicy.this::checkQuotas);
        }
    }
}
