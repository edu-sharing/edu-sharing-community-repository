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
package org.edu_sharing.repository.server.importer;

import java.util.Map;

public interface PersistentHandlerInterface {
	public String safe(Map props, String cursor, String set) throws Throwable;

	public boolean mustBePersisted(String replId, String timeStamp);
}
