package org.edu_sharing.spring.security.server.oauth2.persistence;

import lombok.extern.slf4j.Slf4j;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

@Slf4j
public class AlfrescoOAuth2AuthorizationConsentService implements OAuth2AuthorizationConsentService {

    private final JdbcOAuth2AuthorizationConsentService delegate;
    private final RetryingTransactionHelper transactionHelper;

    public AlfrescoOAuth2AuthorizationConsentService(JdbcOperations jdbcOperations,
                                                     RegisteredClientRepository registeredClientRepository,
                                                     RetryingTransactionHelper transactionHelper){
        this.delegate = new JdbcOAuth2AuthorizationConsentService(jdbcOperations, registeredClientRepository);
        this.transactionHelper = transactionHelper;
    }

    @Override
    public void save(OAuth2AuthorizationConsent authorizationConsent) {
        transactionHelper.doInTransaction(() -> {
            try {
                this.delegate.save(authorizationConsent);
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
            }
            return null;
        });
    }

    @Override
    public void remove(OAuth2AuthorizationConsent authorizationConsent) {
        transactionHelper.doInTransaction(() -> {
            try {
                this.delegate.remove(authorizationConsent);
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
            }
            return null;
        });
    }

    @Override
    public OAuth2AuthorizationConsent findById(String registeredClientId, String principalName) {
        return this.delegate.findById(registeredClientId, principalName);
    }
}
