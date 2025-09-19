package org.edu_sharing.spring.security.server.oauth2.persistence;

import lombok.extern.slf4j.Slf4j;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.lob.DefaultLobHandler;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

@Slf4j
public class AlfrescoOAuth2AuthorizationService implements OAuth2AuthorizationService {

    private final JdbcOAuth2AuthorizationService delegate;
    private final RetryingTransactionHelper transactionHelper;

    public AlfrescoOAuth2AuthorizationService(JdbcTemplate jdbcTemplate,
                                              RegisteredClientRepository registeredClientRepository,
                                              RetryingTransactionHelper transactionHelper) {
        this.delegate = new JdbcOAuth2AuthorizationService(jdbcTemplate, registeredClientRepository);
        this.transactionHelper = transactionHelper;
    }

    @Override
    public void save(OAuth2Authorization authorization) {
        transactionHelper.doInTransaction(() -> {
            try {
                delegate.save(authorization);
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
            }
            return null;
        });
    }

    @Override
    public void remove(OAuth2Authorization authorization) {
        transactionHelper.doInTransaction(() -> {
            try {
                delegate.remove(authorization);
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
            }
            return null;
        });
    }

    @Override
    public OAuth2Authorization findById(String id) {
        return delegate.findById(id);
    }

    @Override
    public OAuth2Authorization findByToken(String token, OAuth2TokenType tokenType) {
        return delegate.findByToken(token, tokenType);
    }
}
