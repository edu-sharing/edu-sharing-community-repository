package org.edu_sharing.repository.server.tools.cache;

import org.edu_sharing.repository.client.rpc.cache.CacheCluster;
import org.edu_sharing.repository.client.rpc.cache.CacheInfo;

public interface CacheManager {
	
	public CacheInfo getCacheInfo(String beanName);
	
	public CacheCluster getCacheCluster();
	
}
