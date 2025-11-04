package org.edu_sharing.spring.security.server.oauth2.persistence;

import lombok.extern.slf4j.Slf4j;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.edu_sharing.spring.conditions.ConditionalOnProperty;
import org.edu_sharing.spring.security.server.oauth2.config.OAuth2ConfigService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

import javax.sql.DataSource;

@Slf4j
@Configuration
@ConditionalOnProperty(name = OAuth2ConfigService.CONFIG_PATH + ".enabled", havingValue = "true")
public class OAuth2SchemaConfiguration {

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @DependsOn("oAuth2SchemaInitializer")
    @Bean
    public OAuth2AuthorizationService authorizationService(
            JdbcTemplate jdbcTemplate,
            RegisteredClientRepository registeredClientRepository,
            RetryingTransactionHelper transactionHelper) {

        log.info("Initializing OAuth2AuthorizationService");
        return new AlfrescoOAuth2AuthorizationService(jdbcTemplate, registeredClientRepository, transactionHelper);
    }
}
