package org.edu_sharing.spring.security.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.HttpMethod;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.edu_sharing.alfresco.policy.NodeCustomizationPolicies;
import org.edu_sharing.alfresco.service.config.model.Config;
import org.edu_sharing.alfresco.service.config.model.LoginSilentMode;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.authentication.AuthenticationFilter;
import org.edu_sharing.repository.server.authentication.ContextManagementFilter;
import org.edu_sharing.service.config.ConfigServiceFactory;
import org.edu_sharing.spring.ApplicationContextFactory;
import org.edu_sharing.spring.security.oauth2.config.OAuth2ClientProperties;
import org.edu_sharing.spring.security.oauth2.config.OAuth2ConfigProvider;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;

import java.util.Optional;


@RequiredArgsConstructor
@Slf4j
@Service
public class SilentLoginModeRedirect {
    public static String SESS_ATT_SILENT_LOGIN_TARGET = "SILENT_LOGIN_TARGET";
    public static String SESS_ATT_SILENT_LOGIN_RESULT = "SILENT_LOGIN_RESULT";

    private final OAuth2ConfigProvider configService;

    public boolean process(HttpServletRequest request, HttpServletResponse response) throws Exception {

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
            log.debug("servlet path {} is not allowed",request.getServletPath());
            return false;
        }

        // check other authentication methods
        if(request.getParameter("ticket") != null
                || request.getHeader("Authorization") != null
                || ContextManagementFilter.accessTool.get() != null
                || request.getParameter(CCConstants.REQUEST_PARAM_ACCESSTOKEN) != null){
            log.debug("another auth method forced");
            return false;
        }


        String result = (String) request.getSession().getAttribute(SESS_ATT_SILENT_LOGIN_RESULT);
        if (result != null) {
            log.debug(SESS_ATT_SILENT_LOGIN_RESULT + ": "+result);
            request.getSession().removeAttribute(SESS_ATT_SILENT_LOGIN_RESULT);
            return false;
        }

        String targetPath = request.getContextPath()
                + request.getServletPath()
                + (request.getPathInfo() != null ? request.getPathInfo() : "")
                + (request.getQueryString() != null ? ("?" + request.getQueryString()) : "");
        request.getSession().setAttribute(SESS_ATT_SILENT_LOGIN_TARGET, targetPath);

        if(targetPath.contains("components/error")){
            log.debug("target path "+targetPath);
            return false;
        }

        log.debug("SILENT_LOGIN_TARGET:"+request.getSession().getAttribute(SESS_ATT_SILENT_LOGIN_TARGET));
        //response.sendRedirect(request.getContextPath() + SilentLoginAuthorizationRequestResolver.DEFAULT_SILENT_LOGIN_PATH);

        String context = NodeCustomizationPolicies.getEduSharingContext();
        OAuth2ClientProperties config = configService.getConfig(context);
        Optional<String> registrationId = config.getRegistration().keySet().stream().filter(s -> s.contains("openIdConnect")).findFirst();

        if(!registrationId.isPresent()){
            return false;
        }

        response.sendRedirect("/edu-sharing/oauth2/authorization/"+context+"_"+registrationId.get()+"?prompt=none");
        return true;
    }

    private boolean isLocalLoginForced(HttpServletRequest request) {
        return request.getRequestURI().equals(request.getContextPath() + AuthenticationFilter.PATH_LOGIN_ANGULAR)
                && "true".equalsIgnoreCase(request.getParameter("local"));
    }

    public boolean processError(HttpServletRequest request, HttpServletResponse response) throws Exception {
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
        request.getSession().removeAttribute(SESS_ATT_SILENT_LOGIN_TARGET);
        return true;
    }

    public boolean processSuccess(HttpServletRequest request, HttpServletResponse response) throws Exception {
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

    private boolean checkConditions(HttpServletRequest request, HttpServletResponse response) throws Exception {
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
