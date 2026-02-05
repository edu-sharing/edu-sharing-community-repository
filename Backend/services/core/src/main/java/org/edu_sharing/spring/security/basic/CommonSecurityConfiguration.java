package org.edu_sharing.spring.security.basic;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.firewall.StrictHttpFirewall;


@Profile({"basic","samlEnabled","openidEnabled"})
@Configuration
public class CommonSecurityConfiguration {
    @Bean
    public StrictHttpFirewall httpFirewall() {
        StrictHttpFirewall firewall = new StrictHttpFirewall();
        //config for allowing urls like: edu-sharing/rest/rendering/v1/details/brockhaus/%252fjulex%252farticle%252fschule?version=-1
        // disabled -> Brockhaus ids are automatically base64 encoded before exposed to clients
        firewall.setAllowUrlEncodedPercent(false);
        firewall.setAllowUrlEncodedSlash(false);
        return firewall;
    }
}
