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

public interface PersistentHandlerInterface {
	public String safe(RecordHandlerInterfaceBase recordHandler, String cursor, String set) throws Throwable;

	public boolean mustBePersisted(String replId, String timeStamp);
	
	public boolean exists(String replId);
}
