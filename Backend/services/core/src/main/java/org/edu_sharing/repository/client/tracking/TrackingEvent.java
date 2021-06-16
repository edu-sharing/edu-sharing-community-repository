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
package org.edu_sharing.repository.client.tracking;

import java.io.Serializable;
import java.util.Date;


/**
 * A TrackingEvent is used to store all the needed informations 
 * for tracking actions. 
 *  
 * @author thomschke
 *
 */
public class TrackingEvent {

	public static final String VERSION = "1.0"; 
	
	private ACTIVITY activity;
	private CONTEXT_ITEM[] context;
	private PLACE place;
	private String session;
	private long timestamp;
	
	public TrackingEvent() {
		this(null, null, null, null);
	}
	
	public TrackingEvent(ACTIVITY activity, CONTEXT_ITEM[] context, PLACE place, String session) {
		
		this.activity = activity;
		this.context = context;
		this.place = place;
		this.session = session;
		this.timestamp = new Date().getTime();
		
	}
	
	// -- what ---------------------------------------------------------------
	
	/**
	 * The enumeration ACTIVITY specify all possible use cases 
	 * available for tracking events.
	 * 
	 * @author thomschke
	 *
	 */
	public enum ACTIVITY {
		
		 USER_LOGIN 
//		,USER_LOGOUT
		,SEARCH_QUERY
//		,SEARCH_TUNING
//		,RESULT_BROWSE
		,RESULT_FETCH_METADATA
		,RESULT_BACK_TO_LIST
		,NODE_CREATE
		,NODE_UPDATE
		
		// for rendering
		
		,CONTENT_DOWNLOAD
		
	}

	/**
	 * This method returns the use case belongs to this tracking event.
	 * 
	 * @return the use case belongs to this tracking event
	 */
	public ACTIVITY getActivity() {
		return this.activity;
	}
	
	public void setActivity(ACTIVITY activity) {
		this.activity = activity;
	}
	
	// -- where --------------------------------------------------------------
	
	/**
	 * The enumeration PLACE specify all possible places 
	 * (frontend: dialogs, backend: services) 
	 * where activities can performed.
	 * 
	 * @author thomschke
	 *
	 */
	public enum PLACE {
		
		
//		 LOGIN 
//		 MAP
		 EXPLORER
		,SEARCH
		,UPLOAD
		
		// for rendering
		
		,LMS
		,REPOSITORY
	}

	/**
	 * This method returns the place belongs to this tracking event.
	 * 
	 * @return the place belongs to this tracking event
	 */
	public PLACE getPlace() {
		return this.place;
	}

	public void setPlace(PLACE place) {
		this.place = place;
	}
	
	// -- with ---------------------------------------------------------------
	
	/**
	 * The enumeration CONTEXT specify all possible parameters 
	 * to describe the context of tracking events.
	 * 
	 * @author thomschke
	 * 
	 */
	public enum CONTEXT {

//		 APP_ID
//		,APP_MODUL
		 REPOSITORY_ID
//		,USER_ID
		,USER_AGENT
		,SEARCH_QUERY
		,SEARCH_LEVEL
//		,RESULT_CURSOR
		,CONTENT_REFERENCE
		,CONTENT_REFERENCE_REMOTE
//		,CONTENT_VERSION
//		,LMS_REFERENCE
		,NODE_TYPE
		
	}
	
	/**
	 * A context item contains a specific context parameter
	 * belongs to this tracking event. 
	 * 
	 * @author thomschke
	 *
	 */
	public static class CONTEXT_ITEM implements Serializable {
		
		private CONTEXT context;
		private String value;
		
		public CONTEXT_ITEM() {
			this(null, null);
		}

		public CONTEXT_ITEM(CONTEXT context, String value) {
			this.context = context;
			this.value = value;
		}
		
		public CONTEXT getContext() {
			return this.context;
		}
		
		public void setContext(CONTEXT context) {
			this.context = context;
		}
		
		public String getValue() {
			return this.value;
		}
		
		public void setValue(String value) {
			this.value = value;
		}
		
	}
	
	/**
	 * This method returns all context items
	 * belongs to this tracking event.
	 * 
	 * @return all context parameter belongs to this tracking event
	 */
	public CONTEXT_ITEM[] getContext() {
		return this.context;
	}
	
	public void setContext(CONTEXT_ITEM[] context) {
		this.context = context;
	}

	// -- who ----------------------------------------------------------------
	
	/**
	 * This method return the usersession belongs to this tracking event.
	 * 
	 * @return the usersession belongs to this tracking event
	 */
	public String getSession() {
		return this.session;
	}
	
	public void setSession(String session) {
		this.session = session;
	}
	
	// -- when ---------------------------------------------------------------
	
	/**
	 * This method return the timestamp belongs to this tracking event.
	 * 
	 * @return the timestamp belongs to this tracking event
	 */
	public long getTime() {
		return this.timestamp;
	}

	public String toString() {
		
		StringBuffer sb = new StringBuffer();
		
		sb.append(TrackingEvent.class.getName()).append("{");
		
			sb.append(" activity:").append(activity);
			sb.append(" context{");
			
			for (CONTEXT_ITEM i : context) {
				sb.append(" ").append(i.context).append(":").append(i.value);
			}
			
			sb.append("}");

			sb.append(" place:").append(place);
			sb.append(" session:").append(session);
			sb.append(" time:").append(timestamp);
									
		sb.append("}");

		return sb.toString();						
	}				
	
}
