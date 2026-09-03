package org.edu_sharing.spring.security.server.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Reads {@code client_id} from the body of a refresh token request of a public client.
 * <p>
 * Spring's {@code PublicClientAuthenticationConverter} bails out on anything but a pkce token request -
 * {@code OAuth2EndpointUtils.matchesPkceTokenRequest} demands {@code grant_type=authorization_code},
 * {@code code} and {@code code_verifier}. A public client refreshing its token therefore reaches the token
 * endpoint with no client authentication at all, gets rejected by the authorization rules of the filter
 * chain and never sees an oauth2 error. This converter closes that gap so that a client which received a
 * refresh token through {@code forceRefreshToken} can also redeem it.
 *
 * @see PublicClientRefreshTokenAuthenticationProvider which validates what this produces
 */
public final class PublicClientRefreshTokenAuthenticationConverter implements AuthenticationConverter {

    @Override
    public Authentication convert(HttpServletRequest request) {
        if (!"POST".equals(request.getMethod())) {
            return null;
        }
        if (!AuthorizationGrantType.REFRESH_TOKEN.getValue().equals(request.getParameter(OAuth2ParameterNames.GRANT_TYPE))
                || !StringUtils.hasText(request.getParameter(OAuth2ParameterNames.REFRESH_TOKEN))) {
            return null;
        }
        // a client that sends credentials is not ours - leave it to the converters that read them, so
        // that a wrong secret still fails as a wrong secret instead of silently becoming a public client
        if (request.getHeader(HttpHeaders.AUTHORIZATION) != null
                || request.getParameter(OAuth2ParameterNames.CLIENT_SECRET) != null) {
            return null;
        }

        String[] clientIds = request.getParameterValues(OAuth2ParameterNames.CLIENT_ID);
        if (clientIds == null) {
            return null;
        }
        if (clientIds.length != 1 || !StringUtils.hasText(clientIds[0])) {
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_REQUEST);
        }

        Map<String, Object> additionalParameters = new HashMap<>();
        request.getParameterMap().forEach((key, values) -> {
            if (!OAuth2ParameterNames.CLIENT_ID.equals(key)) {
                additionalParameters.put(key, (values.length == 1) ? values[0] : values);
            }
        });

        return new OAuth2ClientAuthenticationToken(clientIds[0], ClientAuthenticationMethod.NONE, null,
                additionalParameters);
    }
}
