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
 * This class is a simple implementation for a tracking buffer.
 * 
 * All entries will be stored in a memory ring buffer,
 * persistent will not be provided!   
 * 
 * @author thomschke
 *
 */
public class MemoryRingBuffer extends TrackingRingBuffer {

	private final TrackingEvent[] ringBuffer;

	public MemoryRingBuffer(int size) {

		super(size);
		this.ringBuffer = new TrackingEvent[size];
		
	}
	
	protected TrackingEvent get(int index) {
		return this.ringBuffer[index];
	}
	
	protected void set(int index, TrackingEvent event) {
		this.ringBuffer[index] = event;
	}

}
