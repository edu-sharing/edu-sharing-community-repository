package org.edu_sharing.service.nodeservice;

import org.apache.commons.collections.map.LRUMap;
import org.apache.lucene.util.cache.Cache;
import org.apache.lucene.util.cache.SimpleLRUCache;
import org.edu_sharing.repository.client.tools.CCConstants;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class NodeServiceAdapterCached extends NodeServiceAdapter{
    private static final Cache propertyCache = Cache.synchronizedCache(new SimpleLRUCache(1000));

    public NodeServiceAdapterCached(String appId) {
        super(appId);
    }
    public static void updateCache(HashMap<String,Object> properties) {
        propertyCache.put(properties.get(CCConstants.SYS_PROP_NODE_UID), properties);
    }
    @Override
    public HashMap<String, Object> getProperties(String storeProtocol, String storeId, String nodeId) throws Throwable {
        if (propertyCache.containsKey(nodeId))
            return (HashMap<String, Object>) propertyCache.get(nodeId);
        return null;
    }
}
