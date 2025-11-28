package org.edu_sharing.spring.security.basic;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.ServletContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SpringSecurityFilterChainChecker {
    private final ServletContext servletContext;

    @PostConstruct
    private void afterInit(){
        if(servletContext.getFilterRegistration("springSecurityFilterChain") == null) {
            log.error("Filter springSecurityFilterChain not available. Check web.xml. SecurityConfiguration will not work.");
        }
    }
}
