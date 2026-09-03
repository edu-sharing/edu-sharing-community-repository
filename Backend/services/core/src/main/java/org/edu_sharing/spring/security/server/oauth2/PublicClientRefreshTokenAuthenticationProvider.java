package org.edu_sharing.spring.security.server.oauth2;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.util.Assert;

/**
 * Authenticates a public client refreshing its token, i.e. what
 * {@link PublicClientRefreshTokenAuthenticationConverter} produced.
 * <p>
 * There is nothing to verify about the client itself - it holds no secret, that is what public means. What
 * carries the request is the refresh token: it is bound to one authorization, it is rotated away on first
 * use (see {@code forceRefreshToken} in {@code OAuth2AuthorizationServerConfig}), and
 * {@code OAuth2RefreshTokenAuthenticationProvider} checks right after this that it belongs to this very
 * client. So this only establishes that the client is one we know, is public, and was allowed to have a
 * refresh token at all.
 * <p>
 * Spring's {@code PublicClientAuthenticationProvider} cannot be reused: it ends in
 * {@code codeVerifierAuthenticator.authenticateRequired(...)}, which rejects every grant that is not
 * {@code authorization_code}. It also has to run <em>after</em> this one for the same reason, which it
 * does - the configurer puts added providers in front of the default ones.
 */
public final class PublicClientRefreshTokenAuthenticationProvider implements AuthenticationProvider {

    private static final String ERROR_URI = "https://datatracker.ietf.org/doc/html/rfc6749#section-3.2.1";

    private final RegisteredClientRepository registeredClientRepository;

    public PublicClientRefreshTokenAuthenticationProvider(RegisteredClientRepository registeredClientRepository) {
        Assert.notNull(registeredClientRepository, "registeredClientRepository cannot be null");
        this.registeredClientRepository = registeredClientRepository;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        OAuth2ClientAuthenticationToken clientAuthentication = (OAuth2ClientAuthenticationToken) authentication;
        if (!ClientAuthenticationMethod.NONE.equals(clientAuthentication.getClientAuthenticationMethod())) {
            return null;
        }
        // a pkce token request is spring's business, not ours
        if (!AuthorizationGrantType.REFRESH_TOKEN.getValue()
                .equals(clientAuthentication.getAdditionalParameters().get(OAuth2ParameterNames.GRANT_TYPE))) {
            return null;
        }

        RegisteredClient registeredClient =
                this.registeredClientRepository.findByClientId(clientAuthentication.getPrincipal().toString());
        if (registeredClient == null) {
            throwInvalidClient(OAuth2ParameterNames.CLIENT_ID);
        }
        if (!registeredClient.getClientAuthenticationMethods().contains(ClientAuthenticationMethod.NONE)
                || !PublicClientRefreshTokenGenerator.isForceRefreshToken(registeredClient)) {
            // either not a public client, or one that never received a refresh token from us
            throwInvalidClient("authentication_method");
        }

        return new OAuth2ClientAuthenticationToken(registeredClient, ClientAuthenticationMethod.NONE, null);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return OAuth2ClientAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private static void throwInvalidClient(String parameterName) {
        throw new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.INVALID_CLIENT,
                "Client authentication failed: " + parameterName, ERROR_URI));
    }
}
