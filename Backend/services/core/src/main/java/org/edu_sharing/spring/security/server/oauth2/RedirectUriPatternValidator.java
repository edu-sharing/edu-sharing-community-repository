package org.edu_sharing.spring.security.server.oauth2;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationContext;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationException;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Validates the {@code redirect_uri} of an authorization request against the patterns configured for the
 * client, on top of the exact matching spring does by default.
 * <p>
 * Rfc 6749 requires pre-registered redirect uris to be compared by exact string matching, which spring
 * implements in {@link OAuth2AuthorizationCodeRequestAuthenticationValidator}. That is not workable for
 * every public client: a firefox extension gets a redirect url containing a uuid that is generated per
 * <em>installation</em>, so it cannot be known when the repository is configured. For those cases a client
 * may declare {@code redirectUriPatterns} - a deliberate relaxation that is only in effect for clients
 * that configure it.
 * <p>
 * Exact matching runs first and unchanged, including the loopback rule of rfc 8252; only a request that
 * spring rejects is matched against the patterns. Pkce is not touched here - the code challenge is
 * validated by the provider itself, independent of this validator.
 *
 * @see #matches(String, String) for the matching rules
 */
@Slf4j
public class RedirectUriPatternValidator implements Consumer<OAuth2AuthorizationCodeRequestAuthenticationContext> {

    /**
     * key the patterns are stored under in {@link ClientSettings}. They live on the client rather than in
     * a registry of their own so that they are replaced together with the client whenever the
     * {@code RegisteredClientRepository} is rebuilt on a config refresh.
     */
    public static final String SETTING_REDIRECT_URI_PATTERNS = "settings.client.edu-sharing.redirect-uri-patterns";

    /**
     * redirect uri registered for a client that declares nothing but {@code redirectUriPatterns}.
     * {@link RegisteredClient} insists on at least one redirect uri for the authorization code grant, and
     * this one can never match a real request: the .invalid tld is reserved by rfc 2606 and never resolves.
     */
    public static final String PATTERN_ONLY_REDIRECT_URI = "https://redirect-uri-pattern-only.invalid/";

    /**
     * a {@code *} in the host part stands for exactly one label, i.e. it matches neither a dot nor a
     * slash. Without that restriction {@code https://*.chromiumapp.org/} would also match the host
     * {@code evil.org} in a uri such as {@code https://evil.org/x.chromiumapp.org/}.
     */
    private static final String HOST_WILDCARD = "[^./]+";

    /**
     * a {@code *} in the path stands for anything within one path segment
     */
    private static final String PATH_WILDCARD = "[^/]*";

    /**
     * Rejects an authorization request asking for the {@code openid} scope.
     * <p>
     * This mirrors the validator spring security adds in
     * {@code OAuth2AuthorizationServerConfigurer.init} whenever openid connect is disabled - which it is
     * here, the {@code .oidc(...)} call in {@link OAuth2AuthorizationServerConfig} is commented out.
     * Spring composes that rule onto its own validator, and setting a validator on the provider replaces
     * the whole composition, so it has to be re-added by {@link #authorizationEndpointValidator()}.
     * Without it a client asking for {@code openid} would silently receive an access token and no
     * id_token at all.
     * <p>
     * <b>Drop this once {@code .oidc(...)} is enabled</b> - spring then adds session tracking instead of
     * this rule, and rejecting the scope would break openid connect.
     */
    static final Consumer<OAuth2AuthorizationCodeRequestAuthenticationContext> OPENID_CONNECT_RESTRICTED =
            authenticationContext -> {
                OAuth2AuthorizationCodeRequestAuthenticationToken authentication =
                        authenticationContext.getAuthentication();
                if (authentication.getScopes().contains(OidcScopes.OPENID)) {
                    OAuth2Error error = new OAuth2Error(OAuth2ErrorCodes.INVALID_SCOPE,
                            "OpenID Connect 1.0 authentication requests are restricted.",
                            "https://datatracker.ietf.org/doc/html/rfc6749#section-4.1.2.1");
                    throw new OAuth2AuthorizationCodeRequestAuthenticationException(error, authentication);
                }
            };

    /**
     * The validator to install on the authorization endpoint: pattern aware redirect uri matching plus
     * every rule spring security would have composed on its own.
     * <p>
     * Installed unconditionally rather than only for clients that configure patterns, so that a client
     * added by a config refresh gets the same treatment - the filter chain itself is built once at
     * startup and would otherwise never pick up newly configured patterns.
     */
    public static Consumer<OAuth2AuthorizationCodeRequestAuthenticationContext> authorizationEndpointValidator() {
        return new RedirectUriPatternValidator().andThen(OPENID_CONNECT_RESTRICTED);
    }

    @Override
    public void accept(OAuth2AuthorizationCodeRequestAuthenticationContext authenticationContext) {
        try {
            OAuth2AuthorizationCodeRequestAuthenticationValidator.DEFAULT_REDIRECT_URI_VALIDATOR
                    .accept(authenticationContext);
        } catch (OAuth2AuthorizationCodeRequestAuthenticationException e) {
            if (!matchesAnyPattern(authenticationContext)) {
                throw e;
            }
        }
        OAuth2AuthorizationCodeRequestAuthenticationValidator.DEFAULT_SCOPE_VALIDATOR.accept(authenticationContext);
    }

    private boolean matchesAnyPattern(OAuth2AuthorizationCodeRequestAuthenticationContext authenticationContext) {
        OAuth2AuthorizationCodeRequestAuthenticationToken authentication = authenticationContext.getAuthentication();
        String requestedRedirectUri = authentication.getRedirectUri();
        if (!StringUtils.hasText(requestedRedirectUri)) {
            // nothing to match - spring rejected the request for a reason other than a mismatch, e.g.
            // because redirect_uri is required but was omitted
            return false;
        }
        RegisteredClient registeredClient = authenticationContext.getRegisteredClient();
        List<String> patterns = getRedirectUriPatterns(registeredClient);
        for (String pattern : patterns) {
            if (matches(pattern, requestedRedirectUri)) {
                log.debug("redirect_uri {} of client {} matches configured pattern {}", requestedRedirectUri,
                        registeredClient.getClientId(), pattern);
                return true;
            }
        }
        if (!patterns.isEmpty()) {
            log.debug("redirect_uri {} of client {} matches none of the configured patterns {}",
                    requestedRedirectUri, registeredClient.getClientId(), patterns);
        }
        return false;
    }

    /**
     * @return the patterns configured for the client, empty when it has none
     */
    public static List<String> getRedirectUriPatterns(RegisteredClient registeredClient) {
        List<String> patterns = registeredClient.getClientSettings().getSetting(SETTING_REDIRECT_URI_PATTERNS);
        return patterns == null ? Collections.emptyList() : patterns;
    }

    /**
     * Matches a redirect uri against a pattern. Both are compared component by component rather than as
     * plain strings, so that a {@code *} cannot escape the component it was written in:
     * <ul>
     *     <li>scheme: equal, ignoring case</li>
     *     <li>host and port: the pattern may contain {@code *}, each standing for exactly one label</li>
     *     <li>path: the pattern may contain {@code *}, each standing for anything within one segment</li>
     *     <li>query: equal, or absent on both sides</li>
     *     <li>fragment: rejected on either side, a redirect uri must not carry one</li>
     *     <li>user info: rejected on either side</li>
     * </ul>
     * A requested uri containing a {@code *} never matches - otherwise a caller could pass the pattern
     * itself as its redirect uri.
     */
    static boolean matches(String pattern, String requestedRedirectUri) {
        if (requestedRedirectUri.contains("*")) {
            return false;
        }
        URI patternUri;
        URI requestedUri;
        try {
            patternUri = new URI(pattern);
            requestedUri = new URI(requestedRedirectUri);
        } catch (URISyntaxException e) {
            return false;
        }
        if (patternUri.getScheme() == null || requestedUri.getScheme() == null
                || !patternUri.getScheme().equalsIgnoreCase(requestedUri.getScheme())) {
            return false;
        }
        if (patternUri.getFragment() != null || requestedUri.getFragment() != null) {
            return false;
        }
        if (!Objects.equals(patternUri.getQuery(), requestedUri.getQuery())) {
            return false;
        }
        String patternAuthority = patternUri.getAuthority();
        String requestedAuthority = requestedUri.getAuthority();
        if (patternAuthority == null || requestedAuthority == null) {
            return false;
        }
        // user info would let a request keep the registered host while pointing the visible part of the
        // url somewhere else, and no real redirect uri uses it
        if (patternAuthority.indexOf('@') >= 0 || requestedUri.getUserInfo() != null) {
            return false;
        }
        if (!requestedAuthority.toLowerCase(Locale.ROOT)
                .matches(globToRegex(patternAuthority.toLowerCase(Locale.ROOT), HOST_WILDCARD))) {
            return false;
        }
        return path(requestedUri).matches(globToRegex(path(patternUri), PATH_WILDCARD));
    }

    private static String path(URI uri) {
        return Objects.toString(uri.getPath(), "");
    }

    /**
     * Turns a glob into a regex, quoting everything that is not a {@code *}.
     */
    private static String globToRegex(String glob, String wildcard) {
        StringBuilder regex = new StringBuilder();
        for (String part : glob.split("\\*", -1)) {
            if (!regex.isEmpty()) {
                regex.append(wildcard);
            }
            regex.append(Pattern.quote(part));
        }
        return regex.toString();
    }
}
