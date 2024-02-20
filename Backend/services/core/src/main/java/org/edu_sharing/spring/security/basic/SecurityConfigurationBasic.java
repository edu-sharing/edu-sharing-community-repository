package org.edu_sharing.spring.security.basic;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Profile("basic")
@EnableWebSecurity(debug = true)
@Configuration
public class SecurityConfigurationBasic {

    @Bean
    SecurityFilterChain app(HttpSecurity http) throws Exception {
        CSRFConfig.config(http);
        return http.build();
    }
}
