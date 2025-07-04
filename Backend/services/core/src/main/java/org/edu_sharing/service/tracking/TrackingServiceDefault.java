package org.edu_sharing.service.tracking;

import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.transaction.TransactionService;
import org.apache.commons.codec.digest.DigestUtils;
import org.edu_sharing.alfresco.repository.server.authentication.Context;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.cache.RepositoryCache;
import org.edu_sharing.service.nodeservice.NodeService;
import org.springframework.beans.factory.annotation.Qualifier;

import jakarta.servlet.http.HttpSession;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public abstract class TrackingServiceDefault implements TrackingService {
    protected org.edu_sharing.service.nodeservice.NodeService nodeService;
    public static Map<EventType, String> EVENT_PROPERTY_MAPPING = new HashMap<>() {{
        put(EventType.DOWNLOAD_MATERIAL, CCConstants.CCM_PROP_TRACKING_DOWNLOADS);
        put(EventType.VIEW_MATERIAL, CCConstants.CCM_PROP_TRACKING_VIEWS);
        put(EventType.VIEW_MATERIAL_EMBEDDED, CCConstants.CCM_PROP_TRACKING_VIEWS);
    }};

    private final TransactionService transactionService;
    private final BehaviourFilter policyBehaviourFilter;


    public TrackingServiceDefault(TransactionService transactionService, @Qualifier("policyBehaviourFilter") BehaviourFilter policyBehaviourFilter) {
        this.transactionService = transactionService;
        this.policyBehaviourFilter = policyBehaviourFilter;
    }

    @Override
    public boolean trackActivityOnUser(String authorityName, EventType type) {
        return false;
    }

    @Override
    public boolean trackActivityOnNode(NodeRef nodeRef, NodeTrackingDetails details, EventType type, String authorityName) {
        if (!LightbendConfigLoader.get().getBoolean("repository.tracking.alfresco")) {
            return false;
        }

        String propertyName = EVENT_PROPERTY_MAPPING.get(type);
        if (propertyName == null) {
            return false;
        }

        String value = AuthenticationUtil.runAsSystem(() -> nodeService.getProperty(nodeRef.getStoreRef().getProtocol(), nodeRef.getStoreRef().getIdentifier(), nodeRef.getId(), propertyName));
        if (value == null) {
            value = "0";
        }

        long valueLong = Long.parseLong(value);
        valueLong++;

        final String finalValue = "" + valueLong;
        AuthenticationUtil.runAsSystem(() -> {
            RetryingTransactionHelper rth = transactionService.getRetryingTransactionHelper();
            rth.doInTransaction((RetryingTransactionCallback<Void>) () -> {
                policyBehaviourFilter.disableBehaviour(nodeRef);
                nodeService.setProperty(nodeRef.getStoreRef().getProtocol(), nodeRef.getStoreRef().getIdentifier(), nodeRef.getId(), propertyName, finalValue, false);
                policyBehaviourFilter.enableBehaviour(nodeRef);
                // change the value in cache
                RepositoryCache cacheService = new RepositoryCache();
                Map<String, Object> cache = cacheService.get(nodeRef.getId());
                if (cache != null) {
                    cache.put(propertyName, finalValue);
                    new RepositoryCache().put(nodeRef.getId(), cache);
                }
                return null;
            });
            return null;
        });
        return true;
    }

    /**
     * remove / anonymize / print the username for tracking
     *
     */
    protected String getTrackedUsername(String username) {
        if (username == null) {
            username = AuthenticationUtil.getFullyAuthenticatedUser();
        }

        UserTrackingMode mode = getUserTrackingMode();
        if (mode.equals(UserTrackingMode.obfuscate)) {
            return DigestUtils.sha1Hex(username);
        } else if (mode.equals(UserTrackingMode.full)) {
            return username;
        } else if (mode.equals(UserTrackingMode.session)) {
            HttpSession session = Context.getCurrentInstance() == null ? null :
                    Context.getCurrentInstance().getRequest().getSession(false);
            if (session != null) {
                return DigestUtils.sha1Hex(session.getId() + username);
            }
        }

        // we need any kind of stable id for tracking, so we'll generate a random, hopefully unique UUID
        return UUID.randomUUID().toString();
    }

    protected UserTrackingMode getUserTrackingMode() {
        String mode = LightbendConfigLoader.get().getString("repository.tracking.userMode");
        if (mode == null)
            return UserTrackingMode.none;
        return UserTrackingMode.valueOf(mode);
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }
}
