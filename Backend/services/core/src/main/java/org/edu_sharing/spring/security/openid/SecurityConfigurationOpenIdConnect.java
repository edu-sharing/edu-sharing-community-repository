package org.edu_sharing.spring.security.openid;

import com.typesafe.config.Config;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.Filter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.service.config.ConfigServiceFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.reflect.Field;

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
                );

        if(config.hasPath("security.sso.disableCsrf") && config.getBoolean("security.sso.disableCsrf")){
            http.csrf(AbstractHttpConfigurer::disable);
        }

        // @formatter:on

        return http.build();
    }

    @Component
    class Patcher{

        @Autowired
        SecurityFilterChain securityFilterChain;
        @PostConstruct
        public void init(){
            for(Filter f : securityFilterChain.getFilters()){
                if(f.getClass().getName().equals("org.springframework.security.config.annotation.web.configurers.oauth2.client.OidcBackChannelLogoutFilter")){
                    try {
                        Class c = Class.forName("org.springframework.security.config.annotation.web.configurers.oauth2.client.OidcBackChannelLogoutFilter");
                        Field fLogoutHandler = c.getDeclaredField("logoutHandler");
                        fLogoutHandler.setAccessible(true);
                        Object oidcBackChannelLogoutHandler = fLogoutHandler.get(f);

                        Class c2 = Class.forName("org.springframework.security.config.annotation.web.configurers.oauth2.client.OidcBackChannelLogoutHandler");
                        Field logoutEndpointName = c2.getDeclaredField("logoutEndpointName");
                        logoutEndpointName.setAccessible(true);
                        logoutEndpointName.set(oidcBackChannelLogoutHandler,"/edu-sharing/logout");

                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    } catch (NoSuchFieldException e) {
                        throw new RuntimeException(e);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    } ;

                }
            }
        }
    }


    private LogoutSuccessHandler oidcLogoutSuccessHandler() {
        OidcClientInitiatedLogoutSuccessHandler oidcLogoutSuccessHandler =
                new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository()){
                    @Override
                    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

                        // Sets the location that the End-User's User Agent will be redirected to
                        // after the logout has been performed at the Provider

                        String successTarget = "/shibboleth";
                        try {
                            successTarget = ConfigServiceFactory.getCurrentConfig().getValue("logout.next", successTarget);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }

                        setPostLogoutRedirectUri(successTarget);
                        super.onLogoutSuccess(request, response, authentication);
                    }
                };
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
