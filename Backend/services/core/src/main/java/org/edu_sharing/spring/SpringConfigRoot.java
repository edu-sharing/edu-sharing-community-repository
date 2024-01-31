package org.edu_sharing.spring;

import org.edu_sharing.spring.security.openid.SecurityConfigurationOpenIdConnect;
import org.edu_sharing.spring.security.saml2.SecurityConfigurationSaml;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

@Configuration
//@PropertySource("classpath:application.properties")
@PropertySource(factory = org.edu_sharing.spring.typesafe.TypesafePropertySourceFactory.class,value = "")
@Import({SecurityConfigurationSaml.class, SecurityConfigurationOpenIdConnect.class})
public class SpringConfigRoot {
}
