package org.edu_sharing.spring.security.google;


import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.edu_sharing.spring.security.basic.GuestCleanupFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.util.List;

@Slf4j
@Configuration
@EnableWebSecurity
@Profile(SecurityConfigGoogleOneTap.PROFILE_ID)
public class SecurityConfigGoogleOneTap {

    public static final String PROFILE_ID = "googleOneTapEnabled";

    private final GoogleIdTokenVerifier googleIdTokenVerifier;

    public SecurityConfigGoogleOneTap(GoogleIdTokenVerifier googleIdTokenVerifier) {
        this.googleIdTokenVerifier = googleIdTokenVerifier;
    }

    @Bean
    public AuthenticationProvider googleAuthenticationProvider() {
        return new GoogleOneTapAuthenticationProvider(this.googleIdTokenVerifier);
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        return new ProviderManager(List.of(googleAuthenticationProvider()));
    }

    @Bean
    public SecurityFilterChain googleOneTapFilterChain(HttpSecurity http, GuestCleanupFilter guestCleanupFilter, GoogleOneTapAuthenticationFilter googleFilter, AuthenticationManager authenticationManager) throws Exception {

        http
                .addFilterAfter(guestCleanupFilter, org.springframework.security.web.context.SecurityContextHolderFilter.class)
                .csrf(AbstractHttpConfigurer::disable)
                .securityMatcher("/login/google/**","/shibboleth")
                .authorizeHttpRequests((authorize) -> authorize
                        //   .requestMatchers("/shibboleth").authenticated()
                        //   .requestMatchers("/**").permitAll()
                        /*
                         * we have to use ant matchers here cause the new spring-security version 6.2
                         * tries to use mvc matchers cause it is in classpath. but we don't use mvc matcher,
                         * which causes NoSuchBeanDefinitionException mvcHandlerMappingIntrospector
                         *
                         * org.springframework.security.config.annotation.web.AbstractRequestMatcherRegistry diff 6.1 vs 6.2
                         */
                        .requestMatchers(new AntPathRequestMatcher("/shibboleth")).authenticated()
                        .requestMatchers(new AntPathRequestMatcher("/**")).permitAll()
                        .requestMatchers("/login/google").permitAll()
                )
                .authenticationManager(authenticationManager)
                .addFilterBefore(googleFilter,
                        UsernamePasswordAuthenticationFilter.class)
                .formLogin(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    GoogleOneTapAuthenticationFilter googleFilter(AuthenticationManager authenticationManager, SecurityContextRepository contextRepository ) {
        GoogleOneTapAuthenticationFilter googleFilter = new GoogleOneTapAuthenticationFilter();
        googleFilter.setAuthenticationManager(authenticationManager);
        googleFilter.setSecurityContextRepository(contextRepository);

       googleFilter.setAuthenticationSuccessHandler((request, response, authentication) -> {
           //response.sendRedirect("/edu-sharing/shibboleth");  // your success URL
           response.setStatus(HttpServletResponse.SC_OK);
        });
        return googleFilter;
    }
}
