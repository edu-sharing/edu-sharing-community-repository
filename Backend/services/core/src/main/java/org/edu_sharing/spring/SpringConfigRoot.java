package org.edu_sharing.spring;

import org.edu_sharing.spring.security.basic.SecurityConfigurationBasic;
import org.edu_sharing.spring.security.openid.SecurityConfigurationOpenIdConnect;
import org.edu_sharing.spring.security.saml2.SecurityConfigurationSaml;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@Import({SecurityConfigurationSaml.class, SecurityConfigurationOpenIdConnect.class, SecurityConfigurationBasic.class})
//component scan to enable edu-sharing custom condition annotations
@ComponentScan(basePackages = {"org.edu_sharing"})
@EnableWebMvc
public class SpringConfigRoot implements WebMvcConfigurer {
}
