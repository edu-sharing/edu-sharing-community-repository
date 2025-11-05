package org.edu_sharing.spring.security.server.oauth2;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.extern.slf4j.Slf4j;
import org.edu_sharing.spring.conditions.ConditionalOnProperty;
import org.edu_sharing.spring.security.oauth2.SecurityConfigurationOAuth2;
import org.edu_sharing.spring.security.saml2.SecurityConfigurationSaml;
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
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
    public SecurityFilterChain authServerSecurityFilterChain(HttpSecurity http) throws Exception {
        log.info("SecurityFilterChain server oauth2 config");


        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
                new OAuth2AuthorizationServerConfigurer();

        http
                .securityMatcher(authorizationServerConfigurer.getEndpointsMatcher()) // nur OAuth2 Endpunkte
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .csrf(csrf -> csrf.ignoringRequestMatchers(authorizationServerConfigurer.getEndpointsMatcher()))
                .exceptionHandling(e ->
                        e.authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint(getLoginPath())))
                .with(authorizationServerConfigurer, Customizer.withDefaults());
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
    public RegisteredClientRepository registeredClientRepository() {
        try {

            List<RegisteredClient> collect = oAuth2ConfigService.getDefaultConfig().getClients().stream()
                    .map(c -> {
                                RegisteredClient.Builder builder = RegisteredClient.withId(c.getClientId())
                                        .clientId(c.getClientId())
                                        .clientSecret(c.getClientSecret())
                                        .clientAuthenticationMethod(new ClientAuthenticationMethod(c.getClientAuthenticationMethod()));
                                if(!c.getRedirectUri().isEmpty()) builder.redirectUri(c.getRedirectUri());
                                if(!c.getExpires().isEmpty()) builder.tokenSettings(TokenSettings.builder()
                                        .accessTokenTimeToLive(Duration.ofHours(Integer.parseInt(c.getExpires())))
                                        .build());
                                c.getAuthorizationGrantTypes().forEach(gt -> builder.authorizationGrantType(new AuthorizationGrantType(gt)));
                                c.getScopes().forEach(builder::scope);
                                return builder.build();
                            }
                    ).collect(Collectors.toList());

            return new InMemoryRegisteredClientRepository(collect);
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
            return null;
        }
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
                new NegatedRequestMatcher(new AntPathRequestMatcher("/shibboleth/**"))
        );
        return requestCache;
    }
}
