package org.edu_sharing.spring.security.basic;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.ServletContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.edu_sharing.spring.security.oauth2.SecurityConfigurationOAuth2;
import org.edu_sharing.spring.security.saml2.SecurityConfigurationSaml;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Profile({SecurityConfigurationOAuth2.PROFILE_ID,SecurityConfigurationSaml.PROFILE_ID,SecurityConfigurationBasic.PROFILE_ID})
@Slf4j
@Component
@RequiredArgsConstructor
public class SpringSecurityFilterChainChecker {
    private final ServletContext servletContext;
    private final Environment env;

    @PostConstruct
    private void afterInit(){
        if(servletContext.getFilterRegistration("springSecurityFilterChain") == null) {
            log.error("Filter springSecurityFilterChain not available. Check web.xml. SecurityConfiguration enabled by one of this profiles: {}  will not work.", env.getActiveProfiles());
        }
    }
}
