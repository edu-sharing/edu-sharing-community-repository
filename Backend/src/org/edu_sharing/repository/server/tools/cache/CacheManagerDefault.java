package org.edu_sharing.repository.server.tools.cache;

import java.util.List;

import org.alfresco.repo.cache.SimpleCache;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.rpc.cache.CacheCluster;
import org.edu_sharing.repository.client.rpc.cache.CacheInfo;

public class CacheManagerDefault implements CacheManager {

	@Override
	public CacheInfo getCacheInfo(String beanName) {
		
		CacheInfo ci = new CacheInfo();
		
		SimpleCache cache = (SimpleCache)AlfAppContextGate.getApplicationContext().getBean(beanName);
		
		
		ci.setSize(cache.getKeys().size());
	
		ci.setName(beanName);
		
		return ci;
	}
	
	@Override
	public CacheCluster getCacheCluster() {
		// TODO Auto-generated method stub
		return null;
	}
}
