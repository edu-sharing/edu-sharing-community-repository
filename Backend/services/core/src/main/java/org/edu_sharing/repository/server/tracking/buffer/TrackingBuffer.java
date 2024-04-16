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
package org.edu_sharing.repository.server.tracking.buffer;

import org.edu_sharing.repository.client.tracking.TrackingEvent;

/**
 * A tracking buffer is responsible for queueing tracking events.
 *  
 * @author thomschke
 *
 */
public interface TrackingBuffer {

	public void enqueue(TrackingEvent event);	
	public TrackingEvent dequeue();
	
	public boolean isEmpty();
	
}
