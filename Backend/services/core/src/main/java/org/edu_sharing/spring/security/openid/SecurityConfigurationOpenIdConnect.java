package org.edu_sharing.spring.security.openid;

import com.typesafe.config.Config;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

@Profile("openidEnabled")
@EnableWebSecurity(debug = true)
@Configuration
public class SecurityConfigurationOpenIdConnect {

    Config config = LightbendConfigLoader.get();

    @Bean
    SecurityFilterChain app(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests((authorize) -> authorize
                     //   .requestMatchers("/shibboleth").authenticated()
                     //   .requestMatchers("/**").permitAll()
                        /**
                         * we have to use ant matchers here cause the new spring-security version 6.2
                         * tries to use mvc matchers cause it is in classpath. but we don't use mvc matcher,
                         * which causes NoSuchBeanDefinitionException mvcHandlerMappingIntrospector
                         *
                         * org.springframework.security.config.annotation.web.AbstractRequestMatcherRegistry diff 6.1 vs 6.2
                         */
                    .requestMatchers(new AntPathRequestMatcher("/shibboleth")).authenticated()
                    .requestMatchers(new AntPathRequestMatcher("/**")).permitAll()
                )

                .oauth2Login(Customizer.withDefaults())
                //frontchannel logout triggerd by edu-sharing gui
                .logout((logout) ->
                        logout.logoutSuccessHandler(oidcLogoutSuccessHandler()))
                //backchannel logout
                .oidcLogout((logout) -> logout
                        .backChannel(Customizer.withDefaults())
                )
                .csrf(AbstractHttpConfigurer::disable);

        // @formatter:on

        return http.build();
    }

    private LogoutSuccessHandler oidcLogoutSuccessHandler() {
        OidcClientInitiatedLogoutSuccessHandler oidcLogoutSuccessHandler =
                new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository());

        // Sets the location that the End-User's User Agent will be redirected to
        // after the logout has been performed at the Provider
        oidcLogoutSuccessHandler.setPostLogoutRedirectUri("{baseUrl}/shibboleth");

        return oidcLogoutSuccessHandler;
    }

    @Bean
    ClientRegistrationRepository clientRegistrationRepository() {
        ClientRegistration clientRegistration = ClientRegistrations
                .fromIssuerLocation(config.getString("security.sso.openIdConnect.issuer"))
                .clientId(config.getString("security.sso.openIdConnect.clientId"))
                .clientSecret(config.getString("security.sso.openIdConnect.secret"))
                .scope("openid")
                .build();
        return new InMemoryClientRegistrationRepository(clientRegistration);
    }

}
