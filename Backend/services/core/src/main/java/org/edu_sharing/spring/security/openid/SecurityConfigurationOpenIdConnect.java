package org.edu_sharing.spring.security.openid;

import com.typesafe.config.Config;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.spring.security.saml2.SecurityConfigurationSaml;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;

@Profile("openidEnabled")
@EnableWebSecurity(debug = true)
@Configuration
public class SecurityConfigurationOpenIdConnect {

    Config config = LightbendConfigLoader.get();

    @Bean
    SecurityFilterChain app(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests((authorize) -> authorize
                        //.anyRequest().authenticated()
                        .requestMatchers("/shibboleth").authenticated()
                        .requestMatchers("/**").permitAll()
                )

                .csrf(AbstractHttpConfigurer::disable).oauth2Login(Customizer.withDefaults());

        // @formatter:on

        return http.build();
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
