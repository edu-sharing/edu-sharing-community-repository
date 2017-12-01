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
 * This class is a simple implementation for a tracking event handler.
 * 
 * All occured events will be pushed to the logging service 
 * provided by TrackingContext!   
 * 
 * @author thomschke
 *
 */
public class LoggingHandler implements TrackingEventHandler {

	private TrackingEventHandlerContext context;
	
	@Override
	public void bind(TrackingEventHandlerContext context) {
		
		this.context = context;

	}

	@Override
	public void unbind() {

		this.context = null;
		
	}

	@Override
	public void performEvent(TrackingEvent event) {

		if (this.context != null) {
			
			this.context.logInfo(event.toString());
			
		}

	}

}
