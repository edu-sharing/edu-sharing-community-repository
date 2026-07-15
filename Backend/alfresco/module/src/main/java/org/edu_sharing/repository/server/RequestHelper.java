package org.edu_sharing.repository.server;

import com.typesafe.config.Config;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;

/**
 * provide information about the current request context and proxy related changes
 */
public class RequestHelper {
    private final ServletRequest request;

    public RequestHelper(ServletRequest request) {
        this.request = request;
    }

    public String getRemoteAddr() {
        if(!isInternalNetworkCall() && getConfig().hasPath("ip") && request instanceof HttpServletRequest servletRequest) {
            return servletRequest.getHeader( getConfig().getString("ip"));
        }
        return request.getRemoteAddr();
    }

    public String getServerName() {
        if(!isInternalNetworkCall() && getConfig().hasPath("host") && request instanceof HttpServletRequest servletRequest) {
            return servletRequest.getHeader( getConfig().getString("host"));
        }
        return request.getServerName();
    }

    private boolean isInternalNetworkCall() {
        return ApplicationInfoList.isInternalPortRequest(request);
    }

    private Config getConfig() {
        return LightbendConfigLoader.get().getConfig("repository.request.proxyHeader");
    }
}
