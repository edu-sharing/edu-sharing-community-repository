package org.edu_sharing.spring.security.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.edu_sharing.alfresco.service.guest.GuestService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * adds an prompt=none to authorization request if request matches configured path
 * https://openid.net/specs/openid-connect-core-1_0.html#AuthRequest
 */
@Slf4j
public class SilentLoginAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    OAuth2AuthorizationRequestResolver defaultAuthorizationRequestResolver;;

    GuestService guestService;

    public SilentLoginAuthorizationRequestResolver(ClientRegistrationRepository clientRegistration,GuestService guestService) {
        this.defaultAuthorizationRequestResolver = new DefaultOAuth2AuthorizationRequestResolver(
                clientRegistration, "/oauth2/authorization");
        this.guestService = guestService;
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        OAuth2AuthorizationRequest authorizationRequest =
                this.defaultAuthorizationRequestResolver.resolve(request);

        boolean isSilentLogin = request.getParameter("prompt") != null && "none".equals(request.getParameter("prompt"));

        if(isSilentLogin && (authorizationRequest != null)){
            return customAuthorizationRequest(authorizationRequest);
        }
        return authorizationRequest;
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        OAuth2AuthorizationRequest authorizationRequest =
                this.defaultAuthorizationRequestResolver.resolve(
                        request, clientRegistrationId);
        boolean isSilentLogin = request.getParameter("prompt") != null && "none".equals(request.getParameter("prompt"));

        if(isSilentLogin && (authorizationRequest != null)){
            return customAuthorizationRequest(authorizationRequest);
        }

        return authorizationRequest;
    }


    private OAuth2AuthorizationRequest customAuthorizationRequest(
            OAuth2AuthorizationRequest authorizationRequest) {

        Map<String, Object> additionalParameters =
                new LinkedHashMap<>(authorizationRequest.getAdditionalParameters());
        additionalParameters.put("prompt", "none");
        return OAuth2AuthorizationRequest.from(authorizationRequest)
                .additionalParameters(additionalParameters)
                .build();
    }
}
