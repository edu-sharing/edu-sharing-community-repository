package org.edu_sharing.spring.security.server.oauth2.persistence;

import lombok.extern.slf4j.Slf4j;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.edu_sharing.spring.security.basic.EduSharingPrincipal;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.jackson.SecurityJacksonModules;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

@Slf4j
public class AlfrescoOAuth2AuthorizationService implements OAuth2AuthorizationService {

    private final JdbcOAuth2AuthorizationService delegate;
    private final RetryingTransactionHelper transactionHelper;

    public AlfrescoOAuth2AuthorizationService(JdbcTemplate jdbcTemplate,
                                              RegisteredClientRepository registeredClientRepository,
                                              RetryingTransactionHelper transactionHelper) {
        JsonMapper jsonMapper = createJsonMapper();
        JdbcOAuth2AuthorizationService delegate =
                new JdbcOAuth2AuthorizationService(jdbcTemplate, registeredClientRepository);
        // the lob handler is left at its default, which is the same new DefaultLobHandler() the
        // two argument constructor above passes to the row mapper it replaces
        delegate.setAuthorizationRowMapper(new JdbcOAuth2AuthorizationService.JsonMapperOAuth2AuthorizationRowMapper(
                registeredClientRepository, jsonMapper));
        delegate.setAuthorizationParametersMapper(
                new JdbcOAuth2AuthorizationService.JsonMapperOAuth2AuthorizationParametersMapper(jsonMapper));
        this.delegate = delegate;
        this.transactionHelper = transactionHelper;
    }

    /**
     * The mapper the authorization is persisted with, widened by the types edu-sharing puts into the
     * authorization attributes.
     * <p>
     * When consent is required, the authorization - including the {@code Authentication} of the user - is
     * written to {@code oauth2_authorization} before the consent page is shown and read back when the user
     * confirms. On the {@code /oauth2server} filter chain that authentication is always the
     * {@code UsernamePasswordAuthenticationToken} holding an {@link EduSharingPrincipal} that
     * {@code AcegiBackedSecurityContextHolderStrategy} builds from the alfresco session, no matter how the
     * user logged in.
     * <p>
     * Reading it back needs the principal to pass the polymorphic type validator. Up to spring security 6
     * it did so implicitly: {@code AllowlistTypeIdResolver} accepted any class carrying jackson
     * annotations, and {@link EduSharingPrincipal} carries {@code @JsonTypeInfo} and {@code @JsonCreator}.
     * Spring security 7 replaced that resolver with a {@code BasicPolymorphicTypeValidator} built from the
     * security modules alone, so the type has to be named explicitly now.
     */
    static JsonMapper createJsonMapper() {
        BasicPolymorphicTypeValidator.Builder typeValidator = BasicPolymorphicTypeValidator.builder()
                // the principal itself
                .allowIfSubType(EduSharingPrincipal.class)
                // its authorities and hashIndicator, whose concrete collection type varies
                .allowIfSubType("java.util.")
                // its salt, declared as Serializable but a string in every alfresco setup - a different
                // type shows up as a denied type id naming the class, add it here when it does
                .allowIfSubType(String.class);
        return JsonMapper.builder()
                .addModules(SecurityJacksonModules.getModules(
                        AlfrescoOAuth2AuthorizationService.class.getClassLoader(), typeValidator))
                .build();
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
