package org.edu_sharing.restservices;

import com.typesafe.config.Config;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.authentication.subsystems.SubsystemChainingAuthenticationService;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.repository.server.authentication.AuthenticationFilter;
import org.edu_sharing.repository.server.authentication.ContextManagementFilter;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.authentication.oauth2.TokenService;
import org.edu_sharing.service.authority.AuthorityService;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.service.authority.AuthorityServiceHelper;
import org.edu_sharing.service.config.ConfigServiceFactory;
import org.edu_sharing.service.toolpermission.ToolPermissionServiceFactory;
import org.edu_sharing.spring.security.basic.CSRFConfig;
import org.edu_sharing.spring.security.server.oauth2.OAuth2TokenService;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ApiAuthenticationFilter implements jakarta.servlet.Filter {

    /**
     * Response header telling the caller whether the request was answered as a
     * real authenticated user ("true") or as a guest/anonymous user ("false").
     * Needed because a guest may still receive a 200 response (with a reduced
     * subset of data) so the HTTP status alone does not reveal the auth state.
     */
    public static final String HEADER_AUTHENTICATED = "X-Edu-Authenticated";

    Logger logger = Logger.getLogger(ApiAuthenticationFilter.class);

    private TokenService tokenService;

    private OAuth2TokenService oAuth2TokenService;

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp,
                         FilterChain chain) throws IOException, ServletException {

        HttpServletRequest httpReq = (HttpServletRequest) req;
        HttpServletResponse httpResp = (HttpServletResponse) resp;

        if ("OPTIONS".equals(httpReq.getMethod())) {
            chain.doFilter(req, resp);
            return;
        }

        HttpSession session = httpReq.getSession(true);
        //session.setMaxInactiveInterval(30);
        AuthenticationToolAPI authTool = AuthenticationToolAPI.getInstance();
        Map<String, String> validatedAuth = authTool.validateAuthentication(session);

        AuthenticationFilter.handleLocale(true, httpReq.getHeader("locale"), httpReq, httpResp);

        String authHdr = httpReq.getHeader("Authorization");

        // always take the header so we can auth when a guest is activated
        if (authHdr != null) {

            if (authHdr.length() > 5 && authHdr.substring(0, 5).equalsIgnoreCase("BASIC")) {
                logger.debug("auth is BASIC");
                validatedAuth = httpBasicAuth(httpReq, authHdr);
                if (validatedAuth != null) {
                    validatedAuth = applyValidatedAuth(authTool, validatedAuth, session, httpReq, httpResp);
                }
            } else if (authHdr.length() > 6 && authHdr.substring(0, 6).equalsIgnoreCase("Bearer")) {

                logger.info("auth is OAuth");

                String accessToken = authHdr.substring(6).trim();

                if(oAuth2TokenService != null){
                    try {
                        String userName = oAuth2TokenService.extractUsername(accessToken);
                        if(userName == null) {
                            httpResp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            httpResp.flushBuffer();
                            httpResp.getWriter().print("Could not verify access token");
                            return;
                        }

                        authTool.authenticateUser(userName,((HttpServletRequest) req).getSession(),CCConstants.AUTH_TYPE_OAUTH);
                        validatedAuth = authTool.validateAuthentication(session);
                        logger.debug("oauth2 username is " + userName);
                    /*Token token = tokenService.getToken(accessToken);

                    if (token != null) {
                        logger.info("oAuthToken:" + token.getAccessToken() + " alfresco ticket:" + token.getTicket());

                        //validate and set current user
                        authTool.storeAuthInfoInSession(
                                token.getUsername(),
                                token.getTicket(),
                                CCConstants.AUTH_TYPE_OAUTH,
                                session);

                        session.setAttribute(CCConstants.AUTH_ACCESS_TOKEN, token.getAccessToken());

                        validatedAuth = authTool.validateAuthentication(session);
                    }*/
                    } catch (Exception ex) {

                        logger.error(ex.getMessage(), ex);
                    }
                }else logger.warn("got oauth token but oauth2 service is not available");
            } else if (authHdr.length() > 10 && authHdr.substring(0, 10).equalsIgnoreCase(CCConstants.AUTH_HEADER_EDU_TICKET)) {
                String ticket = authHdr.substring(10).trim();
                if (authTool.validateTicket(ticket)) {
                    // Force a renew of all toolpermissions since they might have now changed!
                    ToolPermissionServiceFactory.getInstance().invalidateSessionCache();
                    //if its APIClient username is ignored and is figured out with authentication service
                    authTool.storeAuthInfoInSession(authTool.getCurrentUser(), ticket, CCConstants.AUTH_TYPE_TICKET, httpReq.getSession());
                    validatedAuth = authTool.validateAuthentication(session);
                }
            }

        }

        // Second step of 2FA: no Authorization header, but session has a pending-2FA username
        // meaning the password was already validated in the first request
        AuthorityService authorityService = AuthorityServiceFactory.getInstance().getLocalService();
        if (authHdr == null && (validatedAuth == null || authorityService.isGuest())) {
            String pending2FaUsername = (String) session.getAttribute(CCConstants.SESSION_2FA_PENDING_USERNAME);
            if (pending2FaUsername != null && httpReq.getHeader("X-2FA-Token") != null) {
                int twoFaCode = httpReq.getIntHeader("X-2FA-Token");
                if (authorityService.validate2Fa(pending2FaUsername, twoFaCode)) {
                    session.removeAttribute(CCConstants.SESSION_2FA_PENDING_USERNAME);
                    validatedAuth = applyValidatedAuth(authTool, pending2FaUsername, session, httpReq, httpResp);
                } else {
                    httpReq.setAttribute(CCConstants.AUTH_ERROR_STATUS, CCConstants.AUTH_ERROR_STATUS_2FA);
                }
            }
        }

        boolean authenticated = validatedAuth != null && !authorityService.isGuest();
        httpResp.addHeader("Access-Control-Expose-Headers", HEADER_AUTHENTICATED);
        httpResp.setHeader(HEADER_AUTHENTICATED, String.valueOf(authenticated));

        Config accessConfig = LightbendConfigLoader.get().getConfig("security.access");
        List<String> AUTHLESS_ENDPOINTS = Arrays.asList(
                "/authentication",
                "/_about",
                "/config",
                "/register",
                "/sharing/v1/sharing",
                "/lti/v13/oidc/login_initiations",
                "/lti/v13/lti13",
                "/lti/v13/registration/dynamic",
                "/lti/v13/jwks",
                "/lti/v13/details",
                "/ltiplatform/v13/openid-configuration",
                "/ltiplatform/v13/openid-registration",
                "/ltiplatform/v13/content",
                "/ltiplatform/v13/generateLoginInitiationFormResourceLink",
                "/ltiplatform/v13/auth");
        List<String> ADMIN_ENDPOINTS = Arrays.asList("/admin", "/bulk", "/lti/v13/registration/static", "/lti/v13/registration/url");
        List<String> DISABLED_ENDPOINTS = new ArrayList<>();

        try {
            if (!ConfigServiceFactory.getCurrentConfig(httpReq).getValue("register.local", true)) {
                if (ConfigServiceFactory.getCurrentConfig(httpReq).getValue("register.recoverPassword", false)) {
                    DISABLED_ENDPOINTS.add("/register/v1/register");
                    DISABLED_ENDPOINTS.add("/register/v1/activate");
                } else {
                    // disable whole api range
                    DISABLED_ENDPOINTS.add("/register");
                }
            }
        } catch (Exception ignored) {
        }

        boolean noAuthenticationNeeded = false;
        String pathInfo = httpReq.getPathInfo();
        for (String endpoint : AUTHLESS_ENDPOINTS) {
            if (pathInfo == null) {
                continue;
            }

            if (pathInfo.startsWith(endpoint)) {
                noAuthenticationNeeded = true;
                break;
            }
        }
        boolean adminRequired = false;
        for (String endpoint : ADMIN_ENDPOINTS) {
            if (pathInfo == null) {
                continue;
            }

            if (pathInfo.startsWith(endpoint)) {
                adminRequired = true;
                break;
            }
        }

        for (String endpoint : DISABLED_ENDPOINTS) {
            if (pathInfo == null) {
                continue;
            }

            if (pathInfo.startsWith(endpoint)) {
                httpResp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                httpResp.flushBuffer();
                httpResp.getWriter().print("This endpoint is disabled via config");
                return;
            }
        }

        for (Map.Entry<String, Object> endpoint : accessConfig.getObject("endpoints").unwrapped().entrySet()) {
            if (pathInfo == null) {
                continue;
            }
            if (pathInfo.startsWith(endpoint.getKey()) && (
                    endpoint.getValue().toString().equalsIgnoreCase("admin") && !AuthorityServiceHelper.isAdmin()
                    || endpoint.getValue().toString().equalsIgnoreCase("disabled")
            )) {
                httpResp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                httpResp.getWriter().print("This endpoint is disabled via config");
                httpResp.flushBuffer();
                return;
            }
        }

        if (adminRequired && !AuthorityServiceFactory.getInstance().getLocalService().isGlobalAdmin()) {
            httpResp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResp.flushBuffer();
            httpResp.getWriter().print("Admin rights are required for this endpoint");
            return;
        }

        // allow authless calls with AUTH_SINGLE_USE_NODEID by appauth
        boolean trustedAuth = ContextManagementFilter.accessTool != null && ContextManagementFilter.accessTool.get() != null;

        // ignore the auth for the login
        if (validatedAuth == null && (!noAuthenticationNeeded && !trustedAuth)) {
            if (pathInfo != null && pathInfo.equals("/openapi.json")) {
                httpResp.setHeader("WWW-Authenticate", "BASIC realm=\"" + "Edu-Sharing Rest API" + "\"");
            }
            httpResp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResp.flushBuffer();
            return;
        }
        if (pathInfo != null && pathInfo.equals("/openapi.json")) {
            String openApiAccess = accessConfig.getString("openapi");
            if (openApiAccess.equalsIgnoreCase("admin") && !AuthorityServiceHelper.isAdmin() || openApiAccess.equalsIgnoreCase("disabled")) {
                httpResp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                httpResp.flushBuffer();
                httpResp.getWriter().print("This endpoint is disabled via config");
                return;
            }
        }
        // Chain other filters
        chain.doFilter(req, resp);
    }

    private Map<String, String> applyValidatedAuth(AuthenticationToolAPI authTool, Map<String, String> validatedAuth, HttpSession session, HttpServletRequest httpReq, HttpServletResponse httpResp) {
        String successfulAuthMethod = SubsystemChainingAuthenticationService.getSuccessFullAuthenticationMethod();
        String authMethod = ("alfrescoNtlm1".equals(successfulAuthMethod) || "alfinst".equals(successfulAuthMethod))
                ? CCConstants.AUTH_TYPE_DEFAULT
                : CCConstants.AUTH_TYPE + successfulAuthMethod;
        authTool.storeAuthInfoInSession(validatedAuth.get(CCConstants.AUTH_USERNAME), validatedAuth.get(CCConstants.AUTH_TICKET), authMethod, session);
        CSRFConfig.csrfInitCookie(httpReq, httpResp);
        return authTool.validateAuthentication(session);
    }

    private Map<String, String> applyValidatedAuth(AuthenticationToolAPI authTool, String username, HttpSession session, HttpServletRequest httpReq, HttpServletResponse httpResp) {
        authTool.authenticateUser(username, session, CCConstants.AUTH_TYPE_DEFAULT);
        CSRFConfig.csrfInitCookie(httpReq, httpResp);
        // 2fa second step: the session still holds the toolpermissions computed for the guest user
        ToolPermissionServiceFactory.getInstance().invalidateSessionCache();
        return authTool.validateAuthentication(session);
    }

    public static Map<String, String> httpBasicAuth(HttpServletRequest httpReq, String authHdr) {
        // auto-skip 2fa if the request port was from internal (non-exposed) network for script access
        return httpBasicAuth(httpReq, authHdr, String.valueOf(httpReq.getLocalPort()).equals(ApplicationInfoList.getHomeRepository().getPort()));
    }

    public static Map<String, String> httpBasicAuth(HttpServletRequest httpReq, String authHdr, boolean ignore2FA) {
        Map<String, String> validatedAuth = null;
        AuthenticationToolAPI authTool = AuthenticationToolAPI.getInstance();

        // Basic authentication details present

        String basicAuth = new String(java.util.Base64.getDecoder().decode(authHdr.substring(6)), StandardCharsets.ISO_8859_1);

        // Split the username and password

        String username = null;
        String password = null;

        int pos = basicAuth.indexOf(":");
        if (pos != -1) {
            username = basicAuth.substring(0, pos);
            password = basicAuth.substring(pos + 1);
        } else {
            username = basicAuth;
            password = "";
        }

        try {
            // Authenticate the user first to validate the password
            validatedAuth = authTool.createNewSession(username, password);

            // Then check 2FA — password was already confirmed above
            if (validatedAuth != null && !ignore2FA) {
                AuthorityService authorityService = AuthorityServiceFactory.getInstance().getLocalService();
                if (authorityService.is2FaActive(username)) {
                    httpReq.setAttribute(CCConstants.AUTH_ERROR_STATUS, CCConstants.AUTH_ERROR_STATUS_2FA);
                    // Store username so the second request can complete auth with just the 2FA code
                    httpReq.getSession(true).setAttribute(CCConstants.SESSION_2FA_PENDING_USERNAME,
                            validatedAuth.get(CCConstants.AUTH_USERNAME));
                    Logger.getLogger(ApiAuthenticationFilter.class).debug("challenging 2fa for " + username);
                    return null;
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(ApiAuthenticationFilter.class).error(ex.getMessage(), ex);
        }
        return validatedAuth;
    }

    @Override
    public void init(FilterConfig arg0) throws ServletException {

        ApplicationContext eduApplicationContext =
                org.edu_sharing.spring.ApplicationContextFactory.getApplicationContext();

        tokenService = (TokenService) eduApplicationContext.getBean("oauthTokenService");
        try {
            oAuth2TokenService = eduApplicationContext.getBean(OAuth2TokenService.class);
        }catch (NoSuchBeanDefinitionException e){
            logger.info("Oauth2TokenService not found");
        }

    }

}
