package org.edu_sharing.service.scope;

import lombok.extern.slf4j.Slf4j;
import org.alfresco.repo.cache.SimpleCache;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

/**
 * Cache for {@link ScopeService#isPublishedInScope}, the source of the permission check (D2).
 * Alfresco cache eduSharingScopePublishedCache (custom-cache-context.xml, caches.properties), key
 * "&lt;scopeId&gt;|&lt;originalNodeId&gt;", negative results are cached as well. Entries are invalidated by the scope
 * service when an entry changes, the ttl is only the fallback.
 */
@Slf4j
public final class ScopePublishedCache {

    private static volatile SimpleCache<String, Boolean> cache;
    /** the cache bean is missing or not configured (e.g. no cluster.type): work without caching */
    private static volatile boolean unavailable;

    private ScopePublishedCache() {
    }

    @SuppressWarnings("unchecked")
    private static SimpleCache<String, Boolean> getCache() {
        if (cache == null && !unavailable) {
            ApplicationContext context = AlfAppContextGate.getApplicationContext();
            if (context == null) {
                // no alfresco context (e.g. unit tests): no caching
                return null;
            }
            try {
                cache = context.getBean("eduSharingScopePublishedCache", SimpleCache.class);
            } catch (BeansException e) {
                // permission checks and writes must not fail because of the cache
                unavailable = true;
                log.warn("cache eduSharingScopePublishedCache is not available, scope permission checks run without cache: {}", e.getMessage());
            }
        }
        return cache;
    }

    private static String key(String scopeId, String nodeId) {
        return scopeId + "|" + nodeId;
    }

    public static Boolean get(String scopeId, String nodeId) {
        SimpleCache<String, Boolean> c = getCache();
        return c == null ? null : c.get(key(scopeId, nodeId));
    }

    public static void put(String scopeId, String nodeId, boolean published) {
        SimpleCache<String, Boolean> c = getCache();
        if (c != null) {
            c.put(key(scopeId, nodeId), published);
        }
    }

    public static void invalidate(String scopeId, String nodeId) {
        SimpleCache<String, Boolean> c = getCache();
        if (c != null) {
            c.remove(key(scopeId, nodeId));
        }
    }

    /**
     * a whole scope is gone: there is no prefix removal, so everything is dropped
     */
    public static void clear() {
        SimpleCache<String, Boolean> c = getCache();
        if (c != null) {
            c.clear();
        }
    }
}
