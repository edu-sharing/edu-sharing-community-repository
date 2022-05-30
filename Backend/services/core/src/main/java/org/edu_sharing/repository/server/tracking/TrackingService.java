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
package org.edu_sharing.repository.server.tracking;

import java.util.HashMap;

import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tracking.TrackingEvent;
import org.edu_sharing.repository.client.tracking.TrackingEvent.ACTIVITY;
import org.edu_sharing.repository.client.tracking.TrackingEvent.CONTEXT_ITEM;
import org.edu_sharing.repository.client.tracking.TrackingEvent.PLACE;
import org.edu_sharing.repository.server.tracking.buffer.TrackingBuffer;
import org.edu_sharing.repository.server.tracking.collector.TrackingEventHandler;

/**
 * This service is responsible for buffering and delivering tracking events.
 * 
 * @author thomschke
 *
 */
public class TrackingService {

	
	public interface TrackingBufferFactory {		
		public TrackingBuffer getTrackingBuffer();		
	}
	
	private static TrackingBufferFactory FACTORY;
	
	
	public static synchronized void registerBuffer(TrackingBufferFactory factory) {
		
		FACTORY = factory;
	}
	
	public static synchronized TrackingBufferFactory unregisterBuffer() {
		
		TrackingBufferFactory temp = FACTORY;
		FACTORY = null;
		return temp;
	}
	
	private static synchronized TrackingBufferFactory getBuffer() {
		
		return FACTORY;
	}

	/**
	 * Create a new tracking event and put it on a tracking buffer. 
	 *  
	 * @param activity
	 * @param place
	 * @param contextKeys
	 * @param contextValues
	 * @param authInfo
	 */
	public static void track(final ACTIVITY activity, final CONTEXT_ITEM[] context, final PLACE place, final HashMap authInfo) {
	
		TrackingBufferFactory factory = getBuffer();
		if (factory != null) {
		
			final String session = (String) authInfo.get(CCConstants.AUTH_TICKET); 
			
			factory.getTrackingBuffer().enqueue(
					new TrackingEvent(activity, context, place, session)					
			);
		}		
	}
	
	/**
	 * Deliver all occurred tracking events from tracking buffer to an handler.
	 *  
	 * @param handler
	 */
	public static void collect(TrackingEventHandler handler) {
		
		TrackingBufferFactory factory = getBuffer();
		if (factory != null) {
		
			TrackingEvent event = null;			
			while((event = factory.getTrackingBuffer().dequeue()) != null) {
				handler.performEvent(event);
			}
		}
	}
}
