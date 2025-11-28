package org.edu_sharing.spring.security.basic;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.firewall.StrictHttpFirewall;


@Configuration
public class CommonSecurityConfiguration {
    @Bean
    public StrictHttpFirewall httpFirewall() {
        StrictHttpFirewall firewall = new StrictHttpFirewall();
        //config for allowing urls like: edu-sharing/rest/rendering/v1/details/brockhaus/%252fjulex%252farticle%252fschule?version=-1
        firewall.setAllowUrlEncodedPercent(true);
        firewall.setAllowUrlEncodedSlash(true);
        return firewall;
    }
}
