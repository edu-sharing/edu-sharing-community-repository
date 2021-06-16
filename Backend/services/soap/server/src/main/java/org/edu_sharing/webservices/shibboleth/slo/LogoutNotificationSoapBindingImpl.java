package org.edu_sharing.webservices.shibboleth.slo;

import java.io.Serializable;
import java.rmi.RemoteException;

import org.apache.log4j.Logger;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.repository.server.tools.cache.ShibbolethSessionsCache;
import org.edu_sharing.repository.server.tools.security.ShibbolethSessions;
import org.edu_sharing.repository.server.tools.security.ShibbolethSessions.SessionInfo;

public class LogoutNotificationSoapBindingImpl implements LogoutNotification, Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	
	transient Logger logger = Logger.getLogger(LogoutNotificationSoapBindingImpl.class);
	
	@Override
	public OKType logoutNotification(String[] sessionIDs) throws RemoteException {
		
    	if(sessionIDs == null || sessionIDs.length < 1){
    		String message = "shibboleth session is is null or empty";
    		logger.error(message);
    		throw new RemoteException(message);
    	}
    	
    	
    	for(String sId : sessionIDs){
			logger.info("shibboleth session id:"+sId);
			
			if(sId == null || sId.trim().equals("")){
				continue;
			}

			/**
			 * cause the not clustered version of SimpleCache does not have the opportunity for adding an Listener
			 * to an Cache Object(ShibbolethSessionsCache) we have to do the local session cleanup by hand
			 */
			if(ShibbolethSessionsCache.isDefaultCacheType()){
				logger.info("no cluster environment");
				ShibbolethSessionsCache.remove(sId);
				ShibbolethSessions.killTomcatSession(sId);
			}else{
				logger.info("cluster environment");
				ShibbolethSessionsCache.remove(sId);
			}
			
			
    	}
    	
    	logger.info("return new OKType()");
        return new OKType();
	}
	
	
}
