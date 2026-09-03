package org.edu_sharing.spring.security.server.oauth2;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.extern.slf4j.Slf4j;
import org.edu_sharing.spring.conditions.ConditionalOnProperty;
import org.edu_sharing.spring.scope.refresh.annotations.RefreshScope;
import org.edu_sharing.spring.security.basic.GuestCleanupFilter;
import org.edu_sharing.spring.security.context.SecurityContextStrategySwitchFilter;
import org.edu_sharing.spring.security.oauth2.SecurityConfigurationOAuth2;
import org.edu_sharing.spring.security.saml2.SecurityConfigurationSaml;
import org.edu_sharing.spring.security.server.oauth2.config.OAuth2Config;
import org.edu_sharing.spring.security.server.oauth2.config.OAuth2ConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeAuthenticationProvider;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationProvider;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Slf4j
@Configuration
@ConditionalOnProperty(name = OAuth2ConfigService.CONFIG_PATH + ".enabled", havingValue = "true")
@EnableWebSecurity()
public class OAuth2AuthorizationServerConfig {

    @Autowired
    OAuth2ConfigService oAuth2ConfigService;


    @Autowired
    Environment env;

    @Bean
    //@Order(1)
    public SecurityFilterChain authServerSecurityFilterChain(HttpSecurity http, LoginUrlAuthenticationEntryPoint loginUrlAuthenticationEntryPoint, GuestCleanupFilter guestCleanupFilter) throws Exception {
        log.info("SecurityFilterChain server oauth2 config");


        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
                new OAuth2AuthorizationServerConfigurer();

        /*authorizationServerConfigurer
                .oidc(Customizer.withDefaults())
                .deviceAuthorizationEndpoint(Customizer.withDefaults());*/
        http.addFilterBefore(new SecurityContextStrategySwitchFilter(), org.springframework.security.web.context.SecurityContextHolderFilter.class);

        http
                .addFilterAfter(guestCleanupFilter, org.springframework.security.web.context.SecurityContextHolderFilter.class)
                .securityMatcher(new OrRequestMatcher(authorizationServerConfigurer.getEndpointsMatcher(),
                        PathPatternRequestMatcher.withDefaults().matcher("/rest/authentication/v1/oauth2consent"),
                        PathPatternRequestMatcher.withDefaults().matcher("/components/oauth2consent")))
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().authenticated())
                .csrf(csrf -> csrf.ignoringRequestMatchers(authorizationServerConfigurer.getEndpointsMatcher()))
                .exceptionHandling(e ->
                        e.authenticationEntryPoint(loginUrlAuthenticationEntryPoint))
                .with(authorizationServerConfigurer, Customizer.withDefaults());
        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
                .deviceAuthorizationEndpoint(d -> d.verificationUri("/oauth2server/device_verification"))
                .deviceVerificationEndpoint(v -> v
                        .deviceVerificationResponseHandler(new SimpleUrlAuthenticationSuccessHandler("/components/login?device_verification_success=true"))
                )
                .authorizationEndpoint(a -> a
                        .consentPage("/rest/authentication/v1/oauth2consent")
                        // adds pattern matching on top of the exact redirect_uri matching spring does,
                        // for public clients whose redirect uri cannot be registered up front. note that
                        // this replaces the whole validator composition spring built in its own init, so
                        // authorizationEndpointValidator() has to carry those rules along
                        .authenticationProviders(providers -> providers.forEach(provider -> {
                            if (provider instanceof OAuth2AuthorizationCodeRequestAuthenticationProvider codeRequestProvider) {
                                codeRequestProvider.setAuthenticationValidator(
                                        RedirectUriPatternValidator.authorizationEndpointValidator());
                            }
                        })))
                // lets a public client that opted in via forceRefreshToken receive a refresh token. the
                // provider holds its generator final without a setter, so it is rebuilt around a generator
                // that wraps - not replaces - the default one, see PublicClientRefreshTokenGenerator.
                // both shared objects are already populated at this point: OAuth2TokenEndpointConfigurer
                // builds the default providers before it hands them to this consumer
                .tokenEndpoint(t -> t.authenticationProviders(providers -> {
                    OAuth2TokenGenerator<? extends OAuth2Token> defaultTokenGenerator =
                            http.getSharedObject(OAuth2TokenGenerator.class);
                    OAuth2AuthorizationService authorizationService =
                            http.getSharedObject(OAuth2AuthorizationService.class);
                    OAuth2TokenGenerator<OAuth2Token> tokenGenerator =
                            new PublicClientRefreshTokenGenerator(defaultTokenGenerator);
                    providers.replaceAll(provider ->
                            (provider instanceof OAuth2AuthorizationCodeAuthenticationProvider)
                                    ? new OAuth2AuthorizationCodeAuthenticationProvider(authorizationService, tokenGenerator)
                                    : provider);
                }));
        return http.build();
    }


    String getLoginPath(){
        if(Arrays.asList(env.getActiveProfiles()).contains(SecurityConfigurationSaml.PROFILE_ID)){
            return SecurityConfigurationSaml.getLoginPath();
        }
        if(Arrays.asList(env.getActiveProfiles()).contains(SecurityConfigurationOAuth2.PROFILE_ID)){
            return "/shibboleth";
        }
        return "/components/login?next=/shibboleth";
    }


    @Bean
    LoginUrlAuthenticationEntryPoint loginUrlAuthenticationEntryPoint(){
        return new LoginUrlAuthenticationEntryPoint(getLoginPath());
    }


    @RefreshScope
    @Bean
    public RegisteredClientRepository registeredClientRepository() {
        List<RegisteredClient> clients = new ArrayList<>();
        try {
            for (OAuth2Config.Client client : oAuth2ConfigService.getDefaultConfig().getClients()) {
                try {
                    clients.add(toRegisteredClient(client));
                } catch (Throwable e) {
                    // one bad entry used to take every other client down with it, because the whole
                    // mapping ran inside a single try block
                    log.error("oauth2 client {} is misconfigured and will not be available: {}",
                            client.getClientId(), e.getMessage(), e);
                }
            }
        } catch (Throwable e) {
            log.error("could not read the oauth2 client configuration, no client will be available", e);
        }
        if (clients.isEmpty()) {
            log.error("no usable oauth2 client is configured, every authorization request will be rejected");
            return EMPTY_REGISTERED_CLIENT_REPOSITORY;
        }
        return new InMemoryRegisteredClientRepository(clients);
    }

    /**
     * Stands in for the client repository when not a single client could be built.
     * <p>
     * Neither of the obvious alternatives works: {@code InMemoryRegisteredClientRepository} rejects an
     * empty list, which would fail the whole application context, and returning {@code null} makes spring
     * register a {@code NullBean} - the {@link RefreshScope} proxy then fails every single request with
     * {@code IllegalArgumentException: object is not an instance of declaring class}, which says nothing
     * about the actual configuration problem. Finding no client instead produces a plain
     * {@code invalid_client} response.
     */
    private static final RegisteredClientRepository EMPTY_REGISTERED_CLIENT_REPOSITORY = new RegisteredClientRepository() {
        @Override
        public void save(RegisteredClient registeredClient) {
            throw new UnsupportedOperationException("the oauth2 clients of this repository are read from the configuration");
        }

        @Override
        public RegisteredClient findById(String id) {
            return null;
        }

        @Override
        public RegisteredClient findByClientId(String clientId) {
            return null;
        }
    };

    private RegisteredClient toRegisteredClient(OAuth2Config.Client c) {
        RegisteredClient.Builder builder = RegisteredClient.withId(c.getClientId())
                .clientId(c.getClientId())
                .clientAuthenticationMethod(new ClientAuthenticationMethod(c.getClientAuthenticationMethod()));
        if (!c.getClientSecret().isEmpty()) {
            if (c.isPublicClient()) {
                log.warn("client {} authenticates with \"none\" but has a clientSecret configured, ignoring "
                        + "the secret - a public client cannot keep one", c.getClientId());
            } else {
                builder.clientSecret(c.getClientSecret());
            }
        }

        // a public client holds no secret, so the code verifier is the only thing tying the token request
        // back to whoever started the flow. the same goes for a client matching its redirect uri by
        // pattern: pkce is what keeps a code that reached the wrong url from being worth anything
        boolean requireProofKey = c.isRequireProofKey() || c.isPublicClient() || !c.getRedirectUriPatterns().isEmpty();
        if (requireProofKey && !c.isRequireProofKey()) {
            log.info("enforcing pkce for oauth2 client {} ({})", c.getClientId(),
                    c.isPublicClient() ? "public client" : "uses redirectUriPatterns");
        }
        ClientSettings.Builder clientSettings = ClientSettings.builder()
                .requireAuthorizationConsent(c.isRequireConsent())
                .requireProofKey(requireProofKey);
        if (!c.getRedirectUriPatterns().isEmpty()) {
            clientSettings.setting(RedirectUriPatternValidator.SETTING_REDIRECT_URI_PATTERNS,
                    List.copyOf(c.getRedirectUriPatterns()));
        }
        boolean forceRefreshToken = resolveForceRefreshToken(c);
        if (forceRefreshToken) {
            clientSettings.setting(PublicClientRefreshTokenGenerator.SETTING_FORCE_REFRESH_TOKEN, true);
        }
        builder.clientSettings(clientSettings.build());

        Set<String> redirectUris = c.getAllRedirectUris();
        if (redirectUris.isEmpty() && !c.getRedirectUriPatterns().isEmpty()) {
            redirectUris = Set.of(RedirectUriPatternValidator.PATTERN_ONLY_REDIRECT_URI);
        }
        redirectUris.forEach(builder::redirectUri);

        TokenSettings.Builder tokenSettings = TokenSettings.builder();
        if (forceRefreshToken) {
            // rfc 9700 allows a refresh token for a public client only when it is rotated or sender
            // constrained, so rotation comes with the opt-in instead of being a knob that can be left off
            tokenSettings.reuseRefreshTokens(false);
        }
        if (!c.getAccessTokenExpires().isEmpty()) {
            tokenSettings.accessTokenTimeToLive(Duration.parse(c.getAccessTokenExpires()));
        }
        if (!c.getRefreshTokenExpires().isEmpty()) {
            tokenSettings.refreshTokenTimeToLive(Duration.parse(c.getRefreshTokenExpires()));
        }
        builder.tokenSettings(tokenSettings.build());
        c.getAuthorizationGrantTypes().forEach(gt -> builder.authorizationGrantType(new AuthorizationGrantType(gt)));
        c.getScopes().forEach(builder::scope);
        return builder.build();
    }

    /**
     * Whether {@code forceRefreshToken} actually takes effect, warning about the combinations where it
     * does nothing. Resolved once so that the opt-in and the rotation that belongs to it cannot drift
     * apart.
     */
    private boolean resolveForceRefreshToken(OAuth2Config.Client c) {
        if (!c.isForceRefreshToken()) {
            return false;
        }
        if (!c.isPublicClient()) {
            log.warn("oauth2 client {} sets forceRefreshToken but is not public, the setting has no effect "
                    + "- a client authenticating with a secret already receives one", c.getClientId());
            return false;
        }
        if (!c.getAuthorizationGrantTypes().contains(AuthorizationGrantType.REFRESH_TOKEN.getValue())) {
            log.warn("oauth2 client {} sets forceRefreshToken but does not list refresh_token in "
                    + "authorizationGrantTypes, so no refresh token is ever requested", c.getClientId());
            return false;
        }
        return true;
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource() throws Exception {
        return oAuth2ConfigService.getJwkSource();
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        log.info("AuthorizationServerSettings server oauth2 config");
        return AuthorizationServerSettings.builder()
                //prevent oauth2server is using the same base path as oidc client (/oauth2)
                .authorizationEndpoint("/oauth2server/authorize")
                .tokenEndpoint("/oauth2server/token")
                .jwkSetEndpoint("/oauth2server/jwks")
                .oidcUserInfoEndpoint("/oauth2server/userinfo")
                .oidcClientRegistrationEndpoint("/oauth2server/register")
                .deviceAuthorizationEndpoint("/oauth2server/device_authorization_endpoint")
                .deviceVerificationEndpoint("/oauth2server/device_verification")
                .tokenRevocationEndpoint("/oauth2server/revoke")
                .tokenIntrospectionEndpoint("/oauth2server/introspect")
                .build();
    }

    /**
     * prevent shibboleth path will be cached in SPRING_SECURITY_SAVED_REQUEST
     * when oauth2server is unauthenticated and redirects to shibboleth servlet
     * this would prevent redirect back to /oauth2server/authorize after successfull auth
     * @return
     */
    @Bean
    public RequestCache requestCache() {
        HttpSessionRequestCache requestCache = new HttpSessionRequestCache();
        requestCache.setRequestMatcher(
                new NegatedRequestMatcher(PathPatternRequestMatcher.withDefaults().matcher("/shibboleth/**"))
        );
        return requestCache;
    }
}
