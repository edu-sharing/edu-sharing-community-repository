package org.edu_sharing.spring.security.openid;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.HttpMethod;
import org.edu_sharing.alfresco.service.config.model.Config;
import org.edu_sharing.alfresco.service.config.model.LoginSilentMode;
import org.edu_sharing.repository.server.authentication.AuthenticationFilter;
import org.edu_sharing.service.config.ConfigServiceFactory;
import org.edu_sharing.spring.ApplicationContextFactory;
import org.springframework.core.env.Profiles;

public class SilentLoginModeRedirect {
    public static String SESS_ATT_SILENT_LOGIN_TARGET = "SILENT_LOGIN_TARGET";
    public static String SESS_ATT_SILENT_LOGIN_RESULT = "SILENT_LOGIN_RESULT";

    public static boolean process(HttpServletRequest request, HttpServletResponse response) throws Exception {

        if (!checkConditions(request, response)) {
            return false;
        }

        //http://localhost:8080/edu-sharing/components/login?local=true
        if (isLocalLoginForced(request)) {
            return false;
        }

        // preview redirect is problematic since browser seem to load random images from cache on 302 responses
        if (request.getServletPath().equals("/rest") || request.getServletPath().equals("/preview")) {
            return false;
        }


        String result = (String) request.getSession().getAttribute(SESS_ATT_SILENT_LOGIN_RESULT);
        if (result != null) {
            request.getSession().removeAttribute(SESS_ATT_SILENT_LOGIN_RESULT);
            return false;
        }

        request.getSession().setAttribute(SESS_ATT_SILENT_LOGIN_TARGET, (request.getContextPath()
                + request.getServletPath()
                + (request.getPathInfo() != null ? request.getPathInfo() : "")
                + (request.getQueryString() != null ? ("?" + request.getQueryString()) : ""))
        );


        response.sendRedirect(request.getContextPath() + SilentLoginAuthorizationRequestResolver.DEFAULT_SILENT_LOGIN_PATH);
        return true;
    }

    private static boolean isLocalLoginForced(HttpServletRequest request) {
        return request.getRequestURI().equals(request.getContextPath() + AuthenticationFilter.PATH_LOGIN_ANGULAR)
                && "true".equalsIgnoreCase(request.getParameter("local"));
    }

    public static boolean processError(HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (!checkConditions(request, response)) {
            return false;
        }
        String target = (String) request.getSession().getAttribute(SESS_ATT_SILENT_LOGIN_TARGET);
        if (target == null) {
            return false;
        }
        request.getSession().setAttribute(SESS_ATT_SILENT_LOGIN_RESULT, "login_required");
        response.sendRedirect(target);
        return true;
    }

    public static boolean processSuccess(HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (!checkConditions(request, response)) {
            return false;
        }
        String target = (String) request.getSession().getAttribute(SESS_ATT_SILENT_LOGIN_TARGET);
        if (target == null) {
            return false;
        }

        request.getSession().setAttribute(AuthenticationFilter.LOGIN_SUCCESS_REDIRECT_URL, target);
        response.sendRedirect(request.getContextPath() + "/shibboleth");
        return true;
    }

    private static boolean checkConditions(HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (!HttpMethod.GET.equals(request.getMethod())) {
            return false;
        }

        if (!ApplicationContextFactory.getApplicationContext().getEnvironment()
                .acceptsProfiles(Profiles.of(SecurityConfigurationOpenIdConnect.PROFILE_ID))) {
            return false;
        }

        Config config = ConfigServiceFactory.getCurrentConfig();
        if (config != null && !LoginSilentMode.redirect.equals(config.values.loginSilentMode)) {
            return false;
        }


        return true;
    }
}
