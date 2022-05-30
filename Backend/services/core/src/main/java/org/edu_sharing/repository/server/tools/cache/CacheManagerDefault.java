package org.edu_sharing.repository.server.tools.cache;

import java.util.Date;

import org.alfresco.repo.cache.SimpleCache;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.rpc.cache.CacheCluster;
import org.edu_sharing.repository.client.rpc.cache.CacheInfo;

import jdk.nashorn.internal.ir.debug.ObjectSizeCalculator;



public class CacheManagerDefault implements CacheManager {

	@Override
	public CacheInfo getCacheInfo(String beanName) {
		
		CacheInfo ci = new CacheInfo();
		
		SimpleCache cache = (SimpleCache)AlfAppContextGate.getApplicationContext().getBean(beanName);
		
		
		ci.setSize(cache.getKeys().size());
		ci.setSizeInMemory(ObjectSizeCalculator.getObjectSize(cache));
		ci.setName(beanName);
		
		return ci;
	}
	
	@Override
	public CacheCluster getCacheCluster() {
		CacheCluster cacheCluster = new CacheCluster();
		Runtime runtime = Runtime.getRuntime();
		cacheCluster.setFreeMemory(runtime.freeMemory());
		cacheCluster.setTotalMemory(runtime.totalMemory());
		cacheCluster.setAvailableProcessors(runtime.availableProcessors());
		cacheCluster.setMaxMemory(runtime.maxMemory());
		cacheCluster.setTimeStamp(new Date());
		return cacheCluster;
	}
}
