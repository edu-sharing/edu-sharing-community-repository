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

import org.w3c.dom.Node;

public interface RecordHandlerInterface extends RecordHandlerInterfaceBase {
	
	public void handleRecord(Node nodeRecord,String cursor,String set) throws Throwable;

}
