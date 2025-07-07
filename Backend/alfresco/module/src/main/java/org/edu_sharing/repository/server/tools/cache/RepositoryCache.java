/**
 *
 */
package org.edu_sharing.repository.server.tools.cache;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.repo.cache.SimpleCache;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.springframework.beans.BeansException;

import com.google.common.collect.ImmutableSet;

@Slf4j
@RequiredArgsConstructor
public class RepositoryCache implements Cache {

    private final ImmutableSet<String> allowedTypes = ImmutableSet.of(
            CCConstants.CCM_TYPE_TOOLPERMISSION,
            CCConstants.CCM_TYPE_IO,
            CCConstants.CCM_TYPE_MAP,
            CCConstants.CCM_TYPE_MAPRELATION,
            CCConstants.CM_TYPE_FOLDER
    );

    private final SimpleCache<String, Map<String, Object>> eduSharingPropertiesCache;

    public synchronized void put(String nodeId, Map<String, Object> props) {
        String currentType = (String) props.get(CCConstants.NODETYPE);
        if (allowedTypes.contains(currentType)) {
            eduSharingPropertiesCache.put(nodeId, props);
        }
    }

    public Map<String, Object> get(String nodeId) {

        return eduSharingPropertiesCache.get(nodeId);

    }

    public void remove(String nodeId) {
        eduSharingPropertiesCache.remove(nodeId);
        // run as system since it will access node service operations which could cause AccessDenied in usage contexts
        AuthenticationUtil.runAsSystem(() -> {
            PreviewCache.purgeCache(nodeId);
            return null;
        });
    }

    public synchronized void setCache(Map<String, Map<String, Object>> cache) {

        eduSharingPropertiesCache.clear();
        for (Map.Entry<String, Map<String, Object>> entry : cache.entrySet()) {
            eduSharingPropertiesCache.put(entry.getKey(), entry.getValue());
        }

    }

}
