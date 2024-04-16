package org.edu_sharing.service.nodeservice;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;

import org.edu_sharing.repository.client.tools.CCConstants;

import java.util.HashMap;
import java.util.concurrent.ConcurrentMap;

public class NodeServiceAdapterCached extends NodeServiceAdapter{
    private static final ConcurrentMap propertyCache = new ConcurrentLinkedHashMap.Builder()
            .maximumWeightedCapacity(1000)
            .build();

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
