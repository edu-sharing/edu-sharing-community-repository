package org.edu_sharing.spring.security.server.oauth2;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@Configuration
@EnableWebMvc
@ComponentScan(basePackages = "org.edu_sharing.spring")
public class MvcConfig {
    // optional beans like converters
}
