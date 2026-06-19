package org.edu_sharing.spring.security.basic;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfigurationBasic {

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return EduWebSecurityCustomizer.webSecurityCustomizer();
    }


     @Bean
     public MethodSecurityExpressionHandler methodSecurityExpressionHandler(PermissionEvaluator permissionEvaluator) {
        DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
        handler.setPermissionEvaluator(permissionEvaluator);
        return handler;
     }

    @Bean
    @Order
    public SecurityFilterChain basicFilterChain(HttpSecurity http) throws Exception {

        http.securityMatcher(new OrRequestMatcher(new AntPathRequestMatcher("/rest"), new AntPathRequestMatcher("/rest/**")))
                .authorizeHttpRequests(authorize ->
                        /*
                         * we have to use ant matchers here cause the new spring-security version 6.2
                         * tries to use mvc matchers cause it is in classpath. but we don't use mvc matcher,
                         * which causes NoSuchBeanDefinitionException mvcHandlerMappingIntrospector
                         *
                         * org.springframework.security.config.annotation.web.AbstractRequestMatcherRegistry diff 6.1 vs 6.2
                         */
                        // /rest will be ignored because we are in the servlet path already
                        //authorize.requestMatchers("/**").authenticated())
                        authorize.requestMatchers(new AntPathRequestMatcher("/**")).permitAll())
                .securityContext(context -> context.securityContextRepository(securityContextRepository()));
        CSRFConfig.config(http);
        HeadersConfig.config(http);
        return http.build();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }
}
