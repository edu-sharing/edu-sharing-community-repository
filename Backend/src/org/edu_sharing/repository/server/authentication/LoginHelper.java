package org.edu_sharing.repository.server.authentication;

import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.repository.server.authentication.Context;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.remote.Rocketchat;
import org.edu_sharing.repository.server.tools.ApplicationInfo;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

public class LoginHelper {
    static Logger logger = Logger.getLogger(LoginHelper.class);
    public static Map<String,RemoteAuthDescription> getRemoteAuthsForSession() {
        HttpSession session = Context.getCurrentInstance().getRequest().getSession(true);
        if(session.getAttribute(CCConstants.SESSION_REMOTE_AUTHENTICATIONS)!=null){
            return (Map<String, RemoteAuthDescription>) session.getAttribute(CCConstants.SESSION_REMOTE_AUTHENTICATIONS);
        }
        Map<String,RemoteAuthDescription> result=new HashMap<>();
        Rocketchat rocketchat= Rocketchat.init();
        if(rocketchat!=null){
            try {
                result.put(ApplicationInfo.TYPE_ROCKETCHAT, rocketchat.getAuthDescription());
            }catch(Exception e){
                logger.warn("Error authenticating at rocketchat: "+e.getMessage(),e);
            }
        }
        session.setAttribute(CCConstants.SESSION_REMOTE_AUTHENTICATIONS,result);
        return result;
    }
}
