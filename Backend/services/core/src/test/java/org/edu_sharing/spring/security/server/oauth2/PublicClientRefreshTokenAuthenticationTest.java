package org.edu_sharing.spring.security.server.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Spring authenticates a public client only on a pkce token request, so refreshing a token would arrive
 * unauthenticated and be answered with a login redirect instead of an oauth2 error. These two close that
 * gap, and both halves have to stay narrow: the converter must not claim a request that carries client
 * credentials, and the provider must not wave through a client that never was allowed a refresh token.
 */
class PublicClientRefreshTokenAuthenticationTest {

    private static final String CLIENT_ID = "browser-plugin";

    private final PublicClientRefreshTokenAuthenticationConverter converter =
            new PublicClientRefreshTokenAuthenticationConverter();

    // ----- converter -----

    @Test
    @DisplayName("a public refresh request becomes a client authentication token")
    void convertsPublicRefreshRequest() {
        Authentication authentication = converter.convert(refreshRequest(Map.of(
                OAuth2ParameterNames.CLIENT_ID, CLIENT_ID)));

        OAuth2ClientAuthenticationToken token =
                assertInstanceOf(OAuth2ClientAuthenticationToken.class, authentication);
        assertEquals(CLIENT_ID, token.getPrincipal());
        assertEquals(ClientAuthenticationMethod.NONE, token.getClientAuthenticationMethod());
        assertFalse(token.isAuthenticated());
        // the grant type has to survive, the provider decides on it
        assertEquals(AuthorizationGrantType.REFRESH_TOKEN.getValue(),
                token.getAdditionalParameters().get(OAuth2ParameterNames.GRANT_TYPE));
        // ... the client id must not, spring's converters strip it as well
        assertFalse(token.getAdditionalParameters().containsKey(OAuth2ParameterNames.CLIENT_ID));
    }

    @DisplayName("requests that are not an unauthenticated public refresh are left alone")
    @Test
    void ignoresEverythingElse() {
        // a pkce token request - spring's own converter handles it
        assertNull(converter.convert(request("POST", Map.of(
                OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.AUTHORIZATION_CODE.getValue(),
                OAuth2ParameterNames.CODE, "abc",
                OAuth2ParameterNames.CLIENT_ID, CLIENT_ID), Map.of())));
        // a client sending a secret is confidential, whatever it claims
        assertNull(converter.convert(refreshRequest(Map.of(
                OAuth2ParameterNames.CLIENT_ID, CLIENT_ID,
                OAuth2ParameterNames.CLIENT_SECRET, "secret"))));
        assertNull(converter.convert(request("POST", Map.of(
                OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.REFRESH_TOKEN.getValue(),
                OAuth2ParameterNames.REFRESH_TOKEN, "rt",
                OAuth2ParameterNames.CLIENT_ID, CLIENT_ID), Map.of("Authorization", "Basic dXNlcjpwdw=="))));
        // no client id at all
        assertNull(converter.convert(refreshRequest(Map.of())));
        // no refresh token
        assertNull(converter.convert(request("POST", Map.of(
                OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.REFRESH_TOKEN.getValue(),
                OAuth2ParameterNames.CLIENT_ID, CLIENT_ID), Map.of())));
        // the token endpoint is POST only
        assertNull(converter.convert(request("GET", Map.of(
                OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.REFRESH_TOKEN.getValue(),
                OAuth2ParameterNames.REFRESH_TOKEN, "rt",
                OAuth2ParameterNames.CLIENT_ID, CLIENT_ID), Map.of())));
    }

    @Test
    @DisplayName("a repeated client_id is rejected rather than silently taking the first")
    void rejectsDuplicateClientId() {
        Map<String, String[]> parameters = new LinkedHashMap<>();
        parameters.put(OAuth2ParameterNames.GRANT_TYPE, new String[] { AuthorizationGrantType.REFRESH_TOKEN.getValue() });
        parameters.put(OAuth2ParameterNames.REFRESH_TOKEN, new String[] { "rt" });
        parameters.put(OAuth2ParameterNames.CLIENT_ID, new String[] { CLIENT_ID, "other" });

        OAuth2AuthenticationException e = assertThrows(OAuth2AuthenticationException.class,
                () -> converter.convert(multiValueRequest("POST", parameters, Map.of())));
        assertEquals(OAuth2ErrorCodes.INVALID_REQUEST, e.getError().getErrorCode());
    }

    // ----- provider, fed with what the converter produced -----

    @Test
    @DisplayName("an opted in public client is authenticated")
    void authenticatesOptedInPublicClient() {
        RegisteredClient registeredClient = client(ClientAuthenticationMethod.NONE, true);

        Authentication authentication = provider(registeredClient).authenticate(convertedRefreshRequest());

        OAuth2ClientAuthenticationToken token =
                assertInstanceOf(OAuth2ClientAuthenticationToken.class, authentication);
        assertSame(registeredClient, token.getRegisteredClient());
        assertTrue(token.isAuthenticated());
    }

    @Test
    @DisplayName("a client that never was allowed a refresh token cannot use one")
    void rejectsClientWithoutOptIn() {
        assertInvalidClient(client(ClientAuthenticationMethod.NONE, false));
    }

    @Test
    @DisplayName("a confidential client has to authenticate with its secret")
    void rejectsConfidentialClient() {
        assertInvalidClient(client(ClientAuthenticationMethod.CLIENT_SECRET_BASIC, true));
    }

    @Test
    @DisplayName("an unknown client id is an invalid client, not a server error")
    void rejectsUnknownClient() {
        assertInvalidClient(null);
    }

    @Test
    @DisplayName("the pkce token request stays spring's business")
    void ignoresAuthorizationCodeGrant() {
        OAuth2ClientAuthenticationToken authorizationCodeRequest = new OAuth2ClientAuthenticationToken(
                CLIENT_ID, ClientAuthenticationMethod.NONE, null,
                Map.of(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.AUTHORIZATION_CODE.getValue()));

        assertNull(provider(client(ClientAuthenticationMethod.NONE, true)).authenticate(authorizationCodeRequest));
    }

    private void assertInvalidClient(RegisteredClient registeredClient) {
        OAuth2AuthenticationException e = assertThrows(OAuth2AuthenticationException.class,
                () -> provider(registeredClient).authenticate(convertedRefreshRequest()));
        assertEquals(OAuth2ErrorCodes.INVALID_CLIENT, e.getError().getErrorCode());
    }

    /**
     * exactly what the converter hands over, so the two halves are tested against each other
     */
    private Authentication convertedRefreshRequest() {
        return converter.convert(refreshRequest(Map.of(OAuth2ParameterNames.CLIENT_ID, CLIENT_ID)));
    }

    private PublicClientRefreshTokenAuthenticationProvider provider(RegisteredClient registeredClient) {
        return new PublicClientRefreshTokenAuthenticationProvider(new RegisteredClientRepository() {
            @Override
            public void save(RegisteredClient client) {
                throw new UnsupportedOperationException();
            }

            @Override
            public RegisteredClient findById(String id) {
                return findByClientId(id);
            }

            @Override
            public RegisteredClient findByClientId(String clientId) {
                return (registeredClient != null && registeredClient.getClientId().equals(clientId))
                        ? registeredClient : null;
            }
        });
    }

    private RegisteredClient client(ClientAuthenticationMethod method, boolean forceRefreshToken) {
        ClientSettings.Builder clientSettings = ClientSettings.builder();
        if (forceRefreshToken) {
            clientSettings.setting(PublicClientRefreshTokenGenerator.SETTING_FORCE_REFRESH_TOKEN, true);
        }
        RegisteredClient.Builder builder = RegisteredClient.withId(CLIENT_ID)
                .clientId(CLIENT_ID)
                .clientAuthenticationMethod(method)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri(RedirectUriPatternValidator.PATTERN_ONLY_REDIRECT_URI)
                .scope("profile")
                .clientSettings(clientSettings.build());
        if (ClientAuthenticationMethod.CLIENT_SECRET_BASIC.equals(method)) {
            builder.clientSecret("{noop}secret");
        }
        return builder.build();
    }

    private HttpServletRequest refreshRequest(Map<String, String> extraParameters) {
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.REFRESH_TOKEN.getValue());
        parameters.put(OAuth2ParameterNames.REFRESH_TOKEN, "the-refresh-token");
        parameters.putAll(extraParameters);
        return request("POST", parameters, Map.of());
    }

    private HttpServletRequest request(String method, Map<String, String> parameters, Map<String, String> headers) {
        Map<String, String[]> parameterValues = new LinkedHashMap<>();
        parameters.forEach((key, value) -> parameterValues.put(key, new String[] { value }));
        return multiValueRequest(method, parameterValues, headers);
    }

    /**
     * A stub over the handful of request properties the converter reads. Hand rolled on purpose:
     * services/core has no spring-test dependency, and spelling the methods out documents exactly what the
     * converter is allowed to look at.
     */
    private HttpServletRequest multiValueRequest(String method, Map<String, String[]> parameters,
                                                 Map<String, String> headers) {
        Map<String, String[]> parameterMap = new HashMap<>(parameters);
        return (HttpServletRequest) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { HttpServletRequest.class }, (proxy, invoked, args) -> switch (invoked.getName()) {
                    case "getMethod" -> method;
                    case "getHeader" -> headers.get((String) args[0]);
                    case "getParameter" -> {
                        String[] values = parameterMap.get((String) args[0]);
                        yield (values == null || values.length == 0) ? null : values[0];
                    }
                    case "getParameterValues" -> parameterMap.get((String) args[0]);
                    case "getParameterMap" -> parameterMap;
                    case "toString" -> method + " " + parameterMap.keySet();
                    default -> throw new UnsupportedOperationException(
                            "the converter must not depend on " + invoked.getName());
                });
    }
}
