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
package org.edu_sharing.repository.server.tools.cache;

import java.util.Map;

public interface Cache {
	
	public Map<String,Object> get(String nodeId);
	
	public void remove(String nodeId);
	
	public void put(String nodeId, Map props);
}
