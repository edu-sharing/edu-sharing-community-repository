package org.edu_sharing.repository.server.authentication;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.service.guest.GuestConfig;
import org.edu_sharing.alfresco.service.guest.GuestService;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.AuthenticationTool;
import org.edu_sharing.repository.server.RepoFactory;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.config.ConfigServiceFactory;
import org.edu_sharing.service.toolpermission.ToolPermissionServiceFactory;

import java.io.IOException;
import java.util.Map;

public class GuestFilter implements jakarta.servlet.Filter {

    Logger logger = Logger.getLogger(GuestFilter.class);

    private GuestService guestService;

    @Override
    public void init(jakarta.servlet.FilterConfig arg0) throws ServletException {
        guestService = AlfAppContextGate.getApplicationContext().getBean(GuestService.class);
    }

    @Override
    public void destroy() {
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {

        logger.debug("starting");
        HttpServletRequest httpRequest = (HttpServletRequest) req;
        if (httpRequest != null && httpRequest.getPathInfo() != null && httpRequest.getPathInfo().startsWith("/_about/")) {
            chain.doFilter(req, res);
            return;
        }
        //prevent caching cause this leads that the index.html would be delivered without going over the search path which goes over the guest filter
        HttpServletResponse httpresponse = (HttpServletResponse) res;
        // Set the Cache-Control and Expires header
        httpresponse.setHeader("Cache-Control", "no-store");
        httpresponse.setHeader("Expires", "0");
        // Print out the URL we're filtering

        try {
            AuthenticationTool authTool = RepoFactory.getAuthenticationToolInstance(ApplicationInfoList.getHomeRepository().getAppId());
            HttpSession session = httpRequest.getSession(true);
            Map<String, String> authentication = authTool.validateAuthentication(session);

            String guestFilterdisabled = req.getParameter(CCConstants.REQUEST_PARAM_DISABLE_GUESTFILTER);
            String guestFilterHeader = ((HttpServletRequest) req).getHeader("DisableGuest");
            if (guestFilterHeader != null && new Boolean(guestFilterHeader)) {
                logger.info("Guest Filter disabled via header");
            } else if (authentication == null && !new Boolean(guestFilterdisabled)) {

                GuestConfig currentGuestConfig = guestService.getCurrentGuestConfig();

                if (currentGuestConfig != null) {
                    if (!currentGuestConfig.isEnabled()) {
                        logger.debug("guest filter disabled for context " + ConfigServiceFactory.getCurrentDomain());
                    } else {
                        Map<String, String> authInfoGuest = authTool.createNewSession(currentGuestConfig.getUsername(), currentGuestConfig.getPassword());
                        authTool.storeAuthInfoInSession(authInfoGuest.get(CCConstants.AUTH_USERNAME), authInfoGuest.get(CCConstants.AUTH_TICKET), CCConstants.AUTH_TYPE_DEFAULT, session);

                        // prewarm tp session cache
                        ToolPermissionServiceFactory.getInstance().getAllAvailableToolPermissions();
                    }
                } else {
                    logger.debug("no guest defined");
                }
            }
        } catch (Throwable e) {
            logger.info("guest credentials seem to be wrong, check your app-info: " + e.getMessage());
        }

        chain.doFilter(req, res);

    }

}
