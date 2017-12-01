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
package org.edu_sharing.repository.server.tracking.collector;

import org.edu_sharing.repository.client.tracking.TrackingEvent;

/**
 * A TrackingEventHandler is responsible for delivering & storing 
 * all occurred tracking events for later analysis.   
 * 
 * @author thomschke
 *
 */
public interface TrackingEventHandler {

	// -- lifecycle ----------------------------------------------------------
	
	public interface TrackingEventHandlerContext {
		
		public String getParameter(String parameter);
		
		public void logInfo(String message);
		public void logError(Throwable t);
		
	}
	
	/**
	 * This method will performed during startup.
	 * 
	 * @param context
	 */
	public void bind(TrackingEventHandlerContext context);
	
	/**
	 * This method will performed during shutdown. 
	 */
	public void unbind();
	
	// -- tracking -----------------------------------------------------------
	
	/**
	 * This method will performed to deliver an occurred tracking event.
	 * 
	 * @param event
	 */
	public void performEvent(TrackingEvent event);
	
}
