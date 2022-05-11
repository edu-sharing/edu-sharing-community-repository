package org.edu_sharing.repository.server;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tracking.TrackingEvent;
import org.edu_sharing.repository.server.tracking.TrackingService;

public aspect TrackingLogin {
	
	Logger logger = Logger.getLogger(TrackingLogin.class);
		
	/**
	 * Login Tracking
	 */
	pointcut authenticate(MCAlfrescoServiceImpl service, String userName, String password): target(service) && execution(HashMap MCAlfrescoServiceImpl.authenticate(String, String)) && args(userName, password);
	
	after(MCAlfrescoServiceImpl service, String userName, String password) returning (HashMap authInfo): authenticate(service, userName, password) {
		
		try {
		
			logger.info("starting");
			
			TrackingService.track(
				TrackingEvent.ACTIVITY.USER_LOGIN, 
				new TrackingEvent.CONTEXT_ITEM[] {
					new TrackingEvent.CONTEXT_ITEM(TrackingEvent.CONTEXT.USER_AGENT, service.getUserAgent())
				},
				TrackingEvent.PLACE.REPOSITORY, 
				authInfo
			);
			
			logger.info("returning");
			
		} catch (Exception e) {
			logger.error(this, e);
		}
	}
	
	/**
	 * Guest Login Tracking
	 */
	pointcut authenticateByGuest(MCAlfrescoServiceImpl service): target(service) && execution(HashMap MCAlfrescoServiceImpl.authenticateByGuest());
	
	after(MCAlfrescoServiceImpl service) returning (HashMap authInfo): authenticateByGuest(service) {
		
		try {
				
			logger.info("starting");
			
			TrackingService.track(
				TrackingEvent.ACTIVITY.USER_LOGIN, 
				new TrackingEvent.CONTEXT_ITEM[] {
					new TrackingEvent.CONTEXT_ITEM(TrackingEvent.CONTEXT.USER_AGENT, service.getUserAgent())
				},
				TrackingEvent.PLACE.REPOSITORY, 
				authInfo
			);
		
			logger.info("returning");

		} catch (Exception e) {
			logger.error(this, e);
		}
	}	
}
