package org.edu_sharing.repository.server.tools.cache;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.repo.cache.SimpleCache;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;

public class PersonCache {
	public static String TYPE="type";
	static final SimpleCache<String, Map<String,Serializable>> sharedCache = 
			(SimpleCache<String,  Map<String,Serializable>>) 
				AlfAppContextGate.getApplicationContext().getBean("eduSharingPersonCache");
	
	private static Map<String, Serializable> getMap(String authorityName) {
		Map<String, Serializable> data = sharedCache.get(authorityName);
		if(data==null)
			return new HashMap<>();
		return data;
	}
	public static Serializable get(String authorityName,String key) {
		return getMap(authorityName).get(key);
	}
	public static boolean contains(String authorityName,String key) {
		return getMap(authorityName).containsKey(key);
	}
	public static void put(String authorityName,String key,Serializable data) {
		Map<String, Serializable> map = getMap(authorityName);
		map.put(key,data);
		sharedCache.put(authorityName,map);
	}
	public static void reset(String member) {
		sharedCache.remove(member);		
	}
}
