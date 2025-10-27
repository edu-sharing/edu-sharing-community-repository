package org.edu_sharing.spring.security.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.HttpMethod;
import lombok.extern.slf4j.Slf4j;
import org.edu_sharing.alfresco.service.config.model.Config;
import org.edu_sharing.alfresco.service.config.model.LoginSilentMode;
import org.edu_sharing.repository.server.authentication.AuthenticationFilter;
import org.edu_sharing.service.config.ConfigServiceFactory;
import org.edu_sharing.spring.ApplicationContextFactory;
import org.springframework.core.env.Profiles;

@Slf4j
public class SilentLoginModeRedirect {
    public static String SESS_ATT_SILENT_LOGIN_TARGET = "SILENT_LOGIN_TARGET";
    public static String SESS_ATT_SILENT_LOGIN_RESULT = "SILENT_LOGIN_RESULT";

    public static boolean process(HttpServletRequest request, HttpServletResponse response) throws Exception {

        if (!checkConditions(request, response)) {
            log.debug("conditions not given");
            return false;
        }

        //http://localhost:8080/edu-sharing/components/login?local=true
        if (isLocalLoginForced(request)) {
            return false;
        }

        // preview redirect is problematic since browser seem to load random images from cache on 302 responses
        if (request.getServletPath().equals("/rest") || request.getServletPath().equals("/preview")) {
            log.debug("path is rest");
            return false;
        }


        String result = (String) request.getSession().getAttribute(SESS_ATT_SILENT_LOGIN_RESULT);
        if (result != null) {
            log.debug(SESS_ATT_SILENT_LOGIN_RESULT + ": "+result);
            request.getSession().removeAttribute(SESS_ATT_SILENT_LOGIN_RESULT);
            return false;
        }

        request.getSession().setAttribute(SESS_ATT_SILENT_LOGIN_TARGET, (request.getContextPath()
                + request.getServletPath()
                + (request.getPathInfo() != null ? request.getPathInfo() : "")
                + (request.getQueryString() != null ? ("?" + request.getQueryString()) : ""))
        );
        log.debug("SILENT_LOGIN_TARGET:"+request.getSession().getAttribute(SESS_ATT_SILENT_LOGIN_TARGET));
        response.sendRedirect(request.getContextPath() + SilentLoginAuthorizationRequestResolver.DEFAULT_SILENT_LOGIN_PATH);
        return true;
    }

    private static boolean isLocalLoginForced(HttpServletRequest request) {
        return request.getRequestURI().equals(request.getContextPath() + AuthenticationFilter.PATH_LOGIN_ANGULAR)
                && "true".equalsIgnoreCase(request.getParameter("local"));
    }

    public static boolean processError(HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (!checkConditions(request, response)) {
            log.debug("processError: conditions not given");
            return false;
        }
        String target = (String) request.getSession().getAttribute(SESS_ATT_SILENT_LOGIN_TARGET);
        if (target == null) {
            log.debug("processError: missing {} in session attributes",SESS_ATT_SILENT_LOGIN_TARGET);
            return false;
        }
        request.getSession().setAttribute(SESS_ATT_SILENT_LOGIN_RESULT, "login_required");
        log.debug("processError: redirecting to "+target);
        response.sendRedirect(target);
        return true;
    }

    public static boolean processSuccess(HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (!checkConditions(request, response)) {
            log.debug("processSuccess: conditions not given");
            return false;
        }
        String target = (String) request.getSession().getAttribute(SESS_ATT_SILENT_LOGIN_TARGET);
        if (target == null) {
            log.debug("processSuccess: missing {}", SESS_ATT_SILENT_LOGIN_TARGET);
            return false;
        }

        request.getSession().setAttribute(AuthenticationFilter.LOGIN_SUCCESS_REDIRECT_URL, target);
        log.debug("processSuccess: set LOGIN_SUCCESS_REDIRECT_URL to {} and redirecting to /shibboleth", target);
        response.sendRedirect(request.getContextPath() + "/shibboleth");
        return true;
    }

    private static boolean checkConditions(HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (!HttpMethod.GET.equals(request.getMethod())) {
            return false;
        }

        if (!ApplicationContextFactory.getApplicationContext().getEnvironment()
                .acceptsProfiles(Profiles.of(SecurityConfigurationOAuth2.PROFILE_ID))) {
            return false;
        }

        Config config = ConfigServiceFactory.getCurrentConfig();
        return config == null || LoginSilentMode.redirect.equals(config.values.loginSilentMode);
    }
}
