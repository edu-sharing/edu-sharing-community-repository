package org.edu_sharing.repository.server.authentication;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.cache.ShibbolethSessionsCache;
import org.edu_sharing.repository.server.tools.security.AllSessions;
import org.edu_sharing.repository.server.tools.security.ShibbolethSessions;
import org.edu_sharing.service.editlock.EditLockServiceFactory;
import org.edu_sharing.service.tracking.TrackingService;
import org.edu_sharing.service.tracking.TrackingServiceFactory;
import org.springframework.context.ApplicationContext;

public class SessionListener implements HttpSessionListener{
	
	Logger logger = Logger.getLogger(SessionListener.class);

	@Override
	public void sessionCreated(HttpSessionEvent event) {
		
		String sessionId = event.getSession().getId();
		AllSessions.put(sessionId, event.getSession());
		
		String ssoSessionId = (String)event.getSession().getAttribute(CCConstants.AUTH_SSO_SESSIONID);
		String ticket = (String)event.getSession().getAttribute(CCConstants.AUTH_TICKET);
		
		logger.debug("new: jsessionid:"+sessionId+" ticket:"+ticket+" ssoSessionId:"+ssoSessionId);
		logger.debug("AllSessions:"+AllSessions.size() + " ShibbolethSessions.size():"+ShibbolethSessions.size() + " ShibbolethSessionsCache.size():"+ShibbolethSessionsCache.size());
	}
	
	@Override
	public void sessionDestroyed(HttpSessionEvent event) {
		trackLogout(event);
		EditLockServiceFactory.getEditLockService().unlockBySession(event.getSession().getId());
				
		//try to clean up Shibboleth Sessions
		if(ShibbolethSessions.size() > 0){
			
			String ssoSessionId = (String)event.getSession().getAttribute(CCConstants.AUTH_SSO_SESSIONID);
			
			if(ssoSessionId != null){
				logger.debug("cleaning up ShibbolethSession for shibsessionId:" + ssoSessionId);
				
				//kill alfresco session
				ShibbolethSessions.SessionInfo sessionInfo = ShibbolethSessions.get(ssoSessionId); 
				
				if(sessionInfo != null){
					logoutWithoutSecurityContext(sessionInfo.getAlfrescoTicket());
				}else{
					logger.warn("no ShibbolethSessions.SessionInfo found for ssoSessionId:"+ssoSessionId);
				}
				
				//clean session map
				ShibbolethSessions.removeInternal(ssoSessionId);
				
				//clean cluster enabled SessionCache (i.e. the tomcat session ended by timeout)
				if(ShibbolethSessionsCache.contains(ssoSessionId)){
					ShibbolethSessionsCache.remove(ssoSessionId);
				}
				
			}
		}
		
		//try to remove Session from AllSessions
		HttpSession removeedSession = AllSessions.remove(event.getSession().getId());
		
		String sessionId = (removeedSession != null) ? removeedSession.getId() : null;
		String ticket = (removeedSession != null) ? (String)removeedSession.getAttribute(CCConstants.AUTH_TICKET) : null;
		String ssoSessionId = (removeedSession != null) ? (String)removeedSession.getAttribute(CCConstants.AUTH_SSO_SESSIONID) : null;
		
		logger.debug("jsessionid:"+sessionId+" ticket:"+ticket+" ssoSessionId:"+ssoSessionId);
		logger.debug("AllSessions:"+AllSessions.size() + " ShibbolethSessions.size():"+ShibbolethSessions.size() + " ShibbolethSessionsCache.size():"+ShibbolethSessionsCache.size());

	}

    private void trackLogout(HttpSessionEvent event) {
        boolean possibleSessionTimeout = (System.currentTimeMillis()-event.getSession().getLastAccessedTime()) >= (event.getSession().getMaxInactiveInterval()*1000);
        String username = (String) event.getSession().getAttribute(CCConstants.AUTH_USERNAME);
        if(username!=null){
            TrackingServiceFactory.getTrackingService().trackActivityOnUser(username,
                    possibleSessionTimeout ? TrackingService.EventType.LOGOUT_USER_TIMEOUT : TrackingService.EventType.LOGOUT_USER_REGULAR);
        }
    }

    public  void logoutWithoutSecurityContext(final String ticket){
		RunAsWork<Void> ra = new RunAsWork<Void>() {
			@Override
			public Void doWork() throws Exception {
				ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
				ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
				AuthenticationService authenticationService = serviceRegistry.getAuthenticationService();
				authenticationService.invalidateTicket(ticket);
				authenticationService.clearCurrentSecurityContext();
				logger.debug("none security context ticket invalidation done");
				return null;
			}
		};
		
		AuthenticationUtil.runAs(ra, "admin");
	}
	
}
