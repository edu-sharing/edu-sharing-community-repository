package org.edu_sharing.repository.server.tools.cache;

import org.edu_sharing.repository.client.rpc.cache.CacheCluster;
import org.edu_sharing.repository.client.rpc.cache.CacheInfo;
import org.edu_sharing.spring.ApplicationContextFactory;


public class CacheManagerFactory {

	
	public static CacheInfo getCacheInfo(String name){
		/*TODO implement for HazelCast*/
		 
		
		/**
		 * cause we have a non clustered version and a clustered version with hazelcast the only unique way to access the cache is the bean name
		 */
		CacheManager cm = (CacheManager)ApplicationContextFactory.getApplicationContext().getBean("esCacheManager");
		
		
		System.out.println("CacheManager class:"+cm.getClass().getName());
		
		return cm.getCacheInfo(name);
		

	}
	
	public static CacheCluster getCacheCluster(){
		CacheManager cm = (CacheManager)ApplicationContextFactory.getApplicationContext().getBean("esCacheManager");
		return cm.getCacheCluster();
	}
	
}
