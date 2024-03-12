package org.edu_sharing.repository.server;

import com.typesafe.config.Config;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

/**
 * provide information about the current request context and proxy related changes
 */
public class RequestHelper {
    private final ServletRequest request;

    public RequestHelper(ServletRequest request) {
        this.request = request;
    }

    public String getRemoteAddr() {
        if(getConfig().hasPath("ip") && request instanceof HttpServletRequest) {
            return ((HttpServletRequest) request).getHeader( getConfig().getString("ip"));
        }
        return request.getRemoteAddr();
    }
    public String getServerName() {
        if(getConfig().hasPath("host") && request instanceof HttpServletRequest) {
            return ((HttpServletRequest) request).getHeader( getConfig().getString("host"));
        }
        return request.getServerName();
    }

    private Config getConfig() {
        return LightbendConfigLoader.get().getConfig("repository.request.proxyHeader");
    }
}
