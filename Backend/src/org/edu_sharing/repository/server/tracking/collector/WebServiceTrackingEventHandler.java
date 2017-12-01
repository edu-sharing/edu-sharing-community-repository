package org.edu_sharing.repository.server.tracking.collector;

import java.rmi.RemoteException;

import org.edu_sharing.repository.client.tracking.TrackingEvent;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.webservices.tracking.TrackingItem;
import org.edu_sharing.webservices.tracking.TrackingItemContext;
import org.edu_sharing.webservices.tracking.TrackingProxy;

public class WebServiceTrackingEventHandler implements TrackingEventHandler {

	private static final String PARAM_ENDPOINT = "endpoint";
	
	private TrackingEventHandlerContext context;	
	private TrackingProxy tracking;
	
	public void bind(TrackingEventHandlerContext context) {
	
		this.context = context;
		this.tracking = new TrackingProxy(context.getParameter(PARAM_ENDPOINT));
		
	}

	public void unbind() {
		
		this.tracking = null;
		this.context = null;
	}

	public void performEvent(TrackingEvent event) {
		
			if (event == null) {
				return;
			}
		
			try {
		
				final TrackingItemContext[] context = new TrackingItemContext[event.getContext().length];
				for ( int i = 0, c = context.length
					; i < c
					; ++i ) {
					
					context[i] = 
							new TrackingItemContext(
									event.getContext()[i].getContext().toString(), 
									event.getContext()[i].getValue()
							);
				}
				
				this.tracking.trackEvent(
						new TrackingItem(
								event.getActivity().toString(),
								ApplicationInfoList.getHomeRepository().getAppId(),
								context,
								event.getPlace().toString(),
								event.getSession(),
								event.getTime(),
								TrackingEvent.VERSION
						));
				
			} catch (RemoteException e) {
				
				this.context.logError(e);
				
			}			

	}

}
