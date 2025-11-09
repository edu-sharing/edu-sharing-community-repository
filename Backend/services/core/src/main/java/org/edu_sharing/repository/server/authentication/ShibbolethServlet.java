/**
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */
package org.edu_sharing.repository.server.authentication;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigValue;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.repo.security.authentication.AuthenticationException;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.UrlTool;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.repository.server.NgServlet;
import org.edu_sharing.repository.server.tools.security.ShibbolethSessions;
import org.edu_sharing.repository.server.tools.security.ShibbolethSessions.SessionInfo;
import org.edu_sharing.service.authentication.EduAuthentication;
import org.edu_sharing.service.authentication.SSOAuthorityMapper;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.service.toolpermission.ToolPermissionService;
import org.edu_sharing.spring.security.oauth2.config.OAuth2ClientProperties;
import org.edu_sharing.spring.servlet.SpringHttpServlet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.extensions.surf.util.URLDecoder;
import org.springframework.extensions.surf.util.URLEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication;
import org.springframework.security.web.savedrequest.DefaultSavedRequest;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class ShibbolethServlet extends SpringHttpServlet {

    //    private SSOAuthorityMapper ssoMapper;
    @Setter(onMethod_ = @Autowired)
    private AuthenticationToolAPI authTool;
    @Setter(onMethod_ = @Autowired)
    private EduAuthentication authService;
    @Setter(onMethod_ = @Autowired)
    private ToolPermissionService toolPermissionService;
    @Setter(onMethod_ = @Autowired)
    private LightbendConfigLoader configLoader;


    private String redirectUrl;


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {


        HttpSession session = req.getSession(true);
        Map<String, String> validAuthInfo = authTool.validateAuthentication(session);

        redirectUrl = (String) session.getAttribute(NgServlet.PREVIOUS_ANGULAR_URL);
        // prefer the login url since it will intercept the regular angular url
        if (session.getAttribute(AuthenticationFilter.LOGIN_SUCCESS_REDIRECT_URL) != null) {
            log.debug("Previous frontend url found: {}", session.getAttribute(AuthenticationFilter.LOGIN_SUCCESS_REDIRECT_URL));
            redirectUrl = (String) session.getAttribute(AuthenticationFilter.LOGIN_SUCCESS_REDIRECT_URL);
        }

        String headerUserName = req.getRemoteUser();
        // headerUserName = getShibValue(ssoMapper.getSSOUsernameProp(), req);//transform(req.getHeader(authMethodShibboleth.getShibbolethUsername()));

		if (validAuthInfo != null ) {
			if(headerUserName == null){
				log.info("no sso username provided, but got valid ticket from session for user:"+validAuthInfo.get(CCConstants.AUTH_USERNAME));
				redirect(resp, req);
				return;
			}else if (validAuthInfo.get(CCConstants.AUTH_USERNAME).equals(headerUserName)) {

				log.info("got valid ticket from session for user:"+headerUserName);
				redirect(resp, req);
				return;

                // do not trigger as guest
                // otherwise, the session will be invalidated but still holding the OIDC token from the user
            } else if (!AuthorityServiceFactory.getInstance().getLocalService().isGuest()) {
                log.info("end session for user:{}", validAuthInfo.get(CCConstants.AUTH_USERNAME));
                authTool.logout(validAuthInfo.get(CCConstants.AUTH_TICKET));
                session.invalidate();
                req.getSession(true);
            }
        }

        try {
            toolPermissionService.invalidateSessionCache();
            log.info("no valid authinfo found in session. Doing the repository shib auth");
            log.info("req.getCharacterEncoding():{}", req.getCharacterEncoding());

            if (req.getCharacterEncoding() == null) {
                req.setCharacterEncoding("UTF-8");
            }

            Map<String, String> ssoMap = new HashMap<>() {{
                put(CCConstants.CM_PROP_PERSON_USERNAME, headerUserName);
            }};
            mapAttributes(ssoMap, req);

            authService.authenticateBySSO(ssoMap);

            String ticket = authService.getCurrentTicket();
            authTool.storeAuthInfoInSession(headerUserName, ticket, CCConstants.AUTH_TYPE_SHIBBOLETH, session);

            String shibbolethSessionId = getShibValue("Shib-Session-ID", req);
            if (StringUtils.isNotBlank(shibbolethSessionId)) {
                ShibbolethSessions.put(shibbolethSessionId, new SessionInfo(ticket, session));
                session.setAttribute(CCConstants.AUTH_SSO_SESSIONID, shibbolethSessionId);
            }

            String referer = req.getHeader(HttpHeaders.REFERER);
            if (StringUtils.isNotBlank(referer)) {
                session.setAttribute(SSOAuthorityMapper.SSO_REFERER, referer);
            }

            redirect(resp, req);

        } catch (org.alfresco.repo.security.authentication.AuthenticationException e) {
            processError(resp, e);
        }
    }

    private static void processError(HttpServletResponse resp, AuthenticationException e) throws IOException {
        String message = e.getMsgId();
        log.error("shibboleth process error:{}", message);
        if (StringUtils.isEmpty(message)) {
            message = "SSO_UNKNOWN_ERROR";
        }
        message = URLEncoder.encode(message.trim());
        resp.sendRedirect("/edu-sharing/components/error/" + message + "/" + message
        );
    }

    private void redirect(HttpServletResponse resp, HttpServletRequest req) throws IOException {

        if (redirectUrl != null) {
            redirectUrl = resp.encodeURL(redirectUrl);
        } else {
            redirectUrl = req.getContextPath();
            Enumeration<String> paramNames = req.getParameterNames();
            while (paramNames.hasMoreElements()) {
                String paramName = paramNames.nextElement();
                String paramVal = req.getParameter(paramName);
                redirectUrl = UrlTool.setParam(redirectUrl, paramName, paramVal);
            }
        }

        log.info("redirectSuccessUrl:{}", redirectUrl);

		UriComponents components = UriComponentsBuilder.fromUriString(redirectUrl).build();
		if(!components.getQueryParams().containsKey("redirectFromSSO")) {
        //so that redirecting to invited trunk works
        //removes trunk param here because it's only necessary cause of anchor is added here (server side does not get anchors)
        redirectUrl = UrlTool.setParamEncode(redirectUrl, "redirectFromSSO", "true");
		}

		DefaultSavedRequest defaultSavedRequest = ((DefaultSavedRequest)req.getSession().getAttribute("SPRING_SECURITY_SAVED_REQUEST"));
		if(defaultSavedRequest != null){
			log.info("using redirect from spring framework:" + defaultSavedRequest.getRedirectUrl());
			redirectUrl = defaultSavedRequest.getRedirectUrl();
		}

		log.info("redirectSuccessUrl:"+redirectUrl);
		resp.sendRedirect(redirectUrl);
	}

    private void mapAttributes(Map<String, String> ssoMap, HttpServletRequest request) {
        boolean externalAuth = configLoader.getConfig().getBoolean("security.sso.external.enabled");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!externalAuth && (authentication == null || !authentication.isAuthenticated())) {
            log.debug("no authentication found or is not authenticated");
            return;
        }

        if (authentication instanceof Saml2Authentication saml2Authentication) {
            ssoMap.put(SSOAuthorityMapper.PARAM_SSO_TYPE, SSOAuthorityMapper.SSO_TYPE_SAML2);

            Saml2AuthenticatedPrincipal saml2AuthenticatedPrincipal = (Saml2AuthenticatedPrincipal) saml2Authentication.getPrincipal();
            saml2AuthenticatedPrincipal.getAttributes().forEach((key, value) -> {
                ssoMap.put(key, value.stream()
                        .findFirst()
                        .map(Object::toString)
                        .orElse(null));
            });

        } else if (authentication instanceof OAuth2AuthenticationToken oath2AuthenticationToken) {
            String authorizedClientRegistrationId = oath2AuthenticationToken.getAuthorizedClientRegistrationId();
            String context = OAuth2ClientProperties.getContextId(authorizedClientRegistrationId);
            String registrationKey = OAuth2ClientProperties.getRegistrationKey(authorizedClientRegistrationId);

            ssoMap.put(SSOAuthorityMapper.PARAM_SSO_TYPE, SSOAuthorityMapper.SSO_TYPE_OAUTH);
            ssoMap.put(SSOAuthorityMapper.PARAM_SSO_OAUTH_CONTEXT, context);
            ssoMap.put(SSOAuthorityMapper.PARAM_SSO_OAUTH_REG_KEY, registrationKey);

            OAuth2User oAuth2User = oath2AuthenticationToken.getPrincipal();
            oAuth2User.getAttributes().forEach((key, value) -> {
                if (value instanceof ArrayList<?> arrayList) {
                    ssoMap.put(key, arrayList.stream()
                            .map(Object::toString)
                            .collect(Collectors.joining(";")));
                } else {
                    ssoMap.put(key, value.toString());
                }
            });
        } else if (externalAuth) {
            ssoMap.put(SSOAuthorityMapper.PARAM_SSO_TYPE, SSOAuthorityMapper.SSO_TYPE_EXTERNAL);
            Config config = this.configLoader.getConfig().getConfig("security.sso.external.mapping.person");
            for(Map.Entry<String, ConfigValue> e : config.entrySet()){
                ssoMap.put(e.getKey(), e.getValue().toString());
            }
            //@TODO additional attributes
        }
    }

    private String getShibValue(String attributeName, HttpServletRequest req) {
        String attValue = (String) req.getAttribute(attributeName);
        if (attValue != null) {
            // see https://wiki.shibboleth.net/confluence/display/SHIB2/NativeSPAttributeAccess#NativeSPAttributeAccess-Tool-SpecificExamples
            attValue = new String(attValue.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
            attValue = URLDecoder.decode(attValue);
        }

        log.info("ShibAtt:{} {}", attributeName, attValue);
        return attValue;
    }
}
