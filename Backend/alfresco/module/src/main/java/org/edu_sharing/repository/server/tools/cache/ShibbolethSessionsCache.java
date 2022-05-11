package org.edu_sharing.repository.server.tools.cache;

import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.alfresco.repo.cache.DefaultSimpleCache;
import org.alfresco.repo.cache.SimpleCache;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;

public class ShibbolethSessionsCache {

	/**
	 * to make SLO work in cluster environment	
	 * 
	 * we don't want to distribute the ShibbolethSessions.SessionInfo object that also contains the tomcat session
	 * also we can find out the repo which really contains this Session (tomcat session is not clustered)
	 * 
	 * so we use this to distribute put and remove events 
	 * 
	 * @return
	 */
	private static SimpleCache<String,String> clusterShibAlfIds = null;
	
	
	
	private static SimpleCache<String,String> getClusterInstance(){
		ReentrantReadWriteLock rw = new ReentrantReadWriteLock(); //Collections.synchronizedMap(new HashMap<K, V>);
		
		
		WriteLock writeLock = rw.writeLock();
		
		
		if(clusterShibAlfIds == null){
			try{
				writeLock.lock();
				//check for null again to prevent the Map is created once mor by a waiter on write lock
				if(clusterShibAlfIds == null){
					
					clusterShibAlfIds = (SimpleCache<String,String>)AlfAppContextGate.getApplicationContext().getBean("eduSharingShibbolethSessionsCache");
				}
				
			}finally{
				writeLock.unlock();
			}
		}
		
		
		return clusterShibAlfIds;
	}
	
	
	public static void put(String shibbolethSessionId, String alfrescoTicket){
		
		 getClusterInstance().put(shibbolethSessionId, alfrescoTicket);
	}
	
	public static void remove(String shibbolethSessionId){
		getClusterInstance().remove(shibbolethSessionId);
	}
	
	public static boolean contains(String shibbolethSessionId){
		return getClusterInstance().contains(shibbolethSessionId);
	}
	
	public static int size(){
		return (getClusterInstance().getKeys() != null) ? getClusterInstance().getKeys().size() : 0;
	}
	
	public static boolean isDefaultCacheType(){
		if(clusterShibAlfIds instanceof DefaultSimpleCache<?, ?>){
			return true;
		}else{
			return false;
		}
	}
	
}
