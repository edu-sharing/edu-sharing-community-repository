/**
 *
 *  
 * 
 * 
 *	
 *
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 *
 */
package org.edu_sharing.repository.server.tools.metadataset;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetBaseProperty;

public class MetadataCache {

	static Map<Integer, MetadataSetBaseProperty> cache = new ConcurrentHashMap<Integer, MetadataSetBaseProperty>();
	
	public static MetadataSetBaseProperty getMetadataSetProperty(Integer id){
		if(id != null) return cache.get(id);
		return null;
	}
	
	public static synchronized void add(MetadataSetBaseProperty prop){
		
		Integer id = prop.getId();
		if(id != null){
			cache.put(id, prop);
		}
		
	}
	
	public void clear(){
		cache.clear();
	}
}
