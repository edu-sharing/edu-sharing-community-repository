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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
public abstract class TrackingRingBuffer implements TrackingBuffer {

	private final Log logger = LogFactory.getLog(TrackingRingBuffer.class);
	private final int maxSize;

	private int first = 0;
	private int last = 0; 
	private int size = 0;
	
	protected TrackingRingBuffer(int maxSize) {
		
		this.maxSize = maxSize;
		
	}
	
	protected void init(int first, int last) {

		synchronized(this) {
	        
			setFirst(first);
			setLast(last);
			
			setSize(first <= last ? last - first : (maxSize - first) + last);
			
		}
	}
	
	public void enqueue(TrackingEvent event) {
		
		synchronized(this) {
        	
			set(getLast(), event);			
            setLast(nextIndex(getLast()));

    		if (getSize() < getMaxSize()) {
    			                
                setSize(getSize() + 1);
                
                int level = getSize() * 100 / getMaxSize(); 

                if (level >= 90) {
                	this.logger.warn("TrackingRingBuffer fill level is very high (" + level + "%), overflow will occur soon!");
                }
                
    		} else {
    			
	    		setFirst(nextIndex(getFirst()));	    		
    			this.logger.error("TrackingRingBuffer is full, overflow occured!");    			
    		}    		
        }		

	}

	public TrackingEvent dequeue() {
		
		synchronized(this) {
			
	    	TrackingEvent event = null;
	
	    	if (0 < getSize()) { 
	
	    		event = get(getFirst());
	    		set(getFirst(), null); // gc
	    		
	            setFirst(nextIndex(getFirst()));       		
	            setSize(getSize() - 1);
	        }
	
	    	return event;
		}
    }


	public boolean isEmpty() {
		return (getSize() == 0);
	}
	
	protected int getMaxSize() {
		return this.maxSize;
	}

	protected abstract TrackingEvent get(int index) ;	
	protected abstract void set(int index, TrackingEvent event);
	
	protected int getFirst() {
		return this.first;
	}
	
	protected void setFirst(int first) {
		this.first = first;
	}
	
	protected int getLast() {
		return this.last;
	}
	
	protected void setLast(int last) {
		this.last = last;
	}

	protected int getSize() {
		return this.size;
	}

	protected void setSize(int size) {
		this.size = size;
	}
	
	private int nextIndex(int index) {		
		return (index + 1) % getMaxSize();		
	}
			
}
