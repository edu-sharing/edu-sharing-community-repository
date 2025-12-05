package org.edu_sharing.spring.security.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.edu_sharing.alfresco.service.guest.GuestService;
import org.edu_sharing.repository.client.tools.UrlTool;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.config.ConfigServiceFactory;
import org.edu_sharing.spring.scope.refresh.annotations.RefreshScope;
import org.edu_sharing.spring.security.basic.*;
import org.edu_sharing.spring.security.oauth2.config.OAuth2ConfigProvider;
import org.edu_sharing.spring.security.openid.persistence.MyBatisOidcSessionRegistry;
import org.edu_sharing.spring.security.openid.persistence.OidcUserSessionMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.oidc.session.OidcSessionRegistry;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.util.UrlUtils;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Configuration
@EnableWebSecurity
@Profile(SecurityConfigurationOAuth2.PROFILE_ID)
public class SecurityConfigurationOAuth2 {


    public static final String PROFILE_ID = "oauth2Enabled";


    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return EduWebSecurityCustomizer.webSecurityCustomizer();
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain oAuth2FilterChain(HttpSecurity http, GuestCleanupFilter guestCleanupFilter, ClientRegistrationRepository clientRegistrationRepository, SilentLoginAuthorizationRequestResolver silentLoginAuthorizationRequestResolver, EduAuthSuccsessHandler eduAuthSuccsessHandler, OidcUserSessionMapper mapper, CustomErrorHandler customErrorHandler) throws Exception {
        http
                .addFilterAfter(guestCleanupFilter, org.springframework.security.web.context.SecurityContextHolderFilter.class)
                .securityMatchers(matchers -> matchers
                        .requestMatchers(new AntPathRequestMatcher("/login/oauth2/**"))
                        .requestMatchers(new AntPathRequestMatcher("/logout/**"))
                        .requestMatchers(new AntPathRequestMatcher("/oauth2"))
                        .requestMatchers(new AntPathRequestMatcher("/oauth2/**"))
                        .requestMatchers(new AntPathRequestMatcher("/shibboleth"))
                        .requestMatchers(new AntPathRequestMatcher("/rest/authentication/v1/validateSSOSession/**"))
                )
                //.securityMatcher("/login/oauth2/**","/logout/**","/oauth2","/oauth2/**","/shibboleth","/rest/authentication/v1/validateSSOSession/**")
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
                )

                .oauth2Login(login -> login
                        //redirect to login page with angular does fallback to default domain, so request attributes not longer available
                        //so it's not useabe in angular dev mode at the moment
                        .loginPage("/sso")
                        .failureHandler(customErrorHandler)
                        .successHandler(eduAuthSuccsessHandler)
                        .authorizationEndpoint(ae -> ae
                                .authorizationRequestResolver(silentLoginAuthorizationRequestResolver)
                                .authorizationRequestRepository(customAuthorizationRequestRepository())))
                .sessionManagement(s -> s.sessionFixation().none())
                //frontchannel logout triggerd by edu-sharing gui
                .logout((logout) -> logout.logoutSuccessHandler(oidcLogoutSuccessHandler(clientRegistrationRepository)))
                //backchannel logout
                .oidcLogout((logout) ->
                        logout.backChannel(bcLogout ->
                                bcLogout.logoutUri(ApplicationInfoList.getHomeRepository().getBaseUrl() + "/edu-sharing/logout"))
                ).headers(h -> h.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable));

        CSRFConfig.config(http);
        HeadersConfig.config(http);

        http.setSharedObject(OidcSessionRegistry.class,new MyBatisOidcSessionRegistry(mapper));
        return http.build();
    }

    private LogoutSuccessHandler oidcLogoutSuccessHandler(ClientRegistrationRepository clientRegistrationRepository) {

        return new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository) {
            @Override
            protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {

                String targetUrl = super.determineTargetUrl(request, response, authentication);


                String idpRedirectUrl;
                String successTarget = "/";
                try {
                    successTarget = ConfigServiceFactory.getCurrentConfig(request).getValue("logout.next", successTarget);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                if (!successTarget.startsWith("http")) {
                            UriComponents successUrlComp = UriComponentsBuilder
                                    .fromHttpUrl(UrlUtils.buildFullRequestUrl(request)).build();

                    idpRedirectUrl = successUrlComp.getScheme() + "://" + successUrlComp.getHost();

                    int port = successUrlComp.getPort();
                    if (port != 80 && port != 443 && port > 0) {
                        idpRedirectUrl += ":" + port;
                    }

                    idpRedirectUrl += request.getContextPath() + successTarget;
                } else {
                    idpRedirectUrl = successTarget;
                }

                return UrlTool.replaceParam(targetUrl, "post_logout_redirect_uri", idpRedirectUrl);
            }
        };
    }

    @Bean
    CustomErrorHandler customErrorHandler(SilentLoginModeRedirect silentLoginModeRedirect) {
        return new CustomErrorHandler(silentLoginModeRedirect);
    }


    @Bean
    public SilentLoginAuthorizationRequestResolver silentLoginAuthorizationRequestResolver(ClientRegistrationRepository clientRegistrationRepository, GuestService guestService) {
        log.info("starting init silentLoginAuthorizationRequestResolver");
        return new SilentLoginAuthorizationRequestResolver(clientRegistrationRepository,guestService);
    }

    @Bean
    public AuthorizationRequestRepository<OAuth2AuthorizationRequest> customAuthorizationRequestRepository() {
        HttpSessionOAuth2AuthorizationRequestRepository wrapped = new HttpSessionOAuth2AuthorizationRequestRepository();
        return new AuthorizationRequestRepository<>() {

            @Override
            public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
                OAuth2AuthorizationRequest oAuth2AuthorizationRequest = wrapped.loadAuthorizationRequest(request);
                if (oAuth2AuthorizationRequest == null) {
                    String parameter = request.getParameter(OAuth2ParameterNames.STATE);
                    if (parameter == null) {
                        log.error("loadAuthorizationRequest returned null cause of missing state parameter");
                    }
                    HttpSession session = request.getSession(false);
                    if (session == null) {
                        log.error("loadAuthorizationRequest returned null cause of session is null");
                    }else{
                        String attName = HttpSessionOAuth2AuthorizationRequestRepository.class
                                .getName() + ".AUTHORIZATION_REQUEST";
                        OAuth2AuthorizationRequest attribute = (OAuth2AuthorizationRequest) session.getAttribute(attName);
                        if (attribute == null) {
                            log.error("loadAuthorizationRequest returned null cause of OAuth2AuthorizationRequest attribute is null");
                        }else{
                            if(parameter != null){
                                if(!parameter.equals(attribute.getState())) {
                                    log.error("loadAuthorizationRequest returned null cause of state param {} != {}", parameter, attribute.getState());
                                }
                            }
                        }
                    }
                }
                return oAuth2AuthorizationRequest;
            }

            @Override
            public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request, HttpServletResponse response) {
                wrapped.saveAuthorizationRequest(authorizationRequest, request, response);
            }

            @Override
            public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request, HttpServletResponse response) {
                return wrapped.removeAuthorizationRequest(request, response);
            }
        };
    }

    @Bean
    @RefreshScope
    public ClientRegistrationRepository clientRegistrationRepository(OAuth2ConfigProvider configService) {
        List<ClientRegistration> registrations = new ArrayList<>(
                new OAuth2ClientPropertiesMapper(configService).asClientRegistrations().values());
        return new InMemoryClientRegistrationRepository(registrations);
    }
}
