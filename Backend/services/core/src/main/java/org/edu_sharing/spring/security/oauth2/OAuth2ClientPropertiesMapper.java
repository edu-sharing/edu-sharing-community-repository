package org.edu_sharing.spring.security.oauth2;

import org.edu_sharing.spring.properties.PropertyMapper;
import org.edu_sharing.spring.security.oauth2.config.OAuth2ClientProperties;
import org.edu_sharing.spring.security.oauth2.config.OAuth2ConfigProvider;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.core.AuthenticationMethod;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record OAuth2ClientPropertiesMapper(OAuth2ConfigProvider oAuth2ConfigProvider) {
    public Map<String, ClientRegistration> asClientRegistrations() {
        Map<String, ClientRegistration> clientRegistrations = new HashMap<>();
        List<OAuth2ClientProperties> allConfigs = oAuth2ConfigProvider.getAllConfigs();
        allConfigs.forEach(config -> {
            config.getRegistration().forEach((key, value) -> {
                String registrationId = config.getRegistrationId(key);
                clientRegistrations.put(registrationId, getClientRegistration(registrationId, key, value, config.getProvider()));
            });
        });


        return clientRegistrations;
    }

    private ClientRegistration getClientRegistration(String registrationId, String registrationKey, OAuth2ClientProperties.Registration properties, Map<String, OAuth2ClientProperties.Provider> providers) {
        ClientRegistration.Builder builder = getBuilderFromIssuerIfPossible(registrationId, registrationKey, properties.getProvider(), providers);
        if (builder == null) {
            builder = getBuilder(registrationId, registrationKey, properties.getProvider(), providers);
        }
        PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
        map.from(properties::getClientId).to(builder::clientId);
        map.from(properties::getClientSecret).to(builder::clientSecret);
        map.from(properties::getClientAuthenticationMethod)
                .as(ClientAuthenticationMethod::new)
                .to(builder::clientAuthenticationMethod);
        map.from(properties::getAuthorizationGrantType)
                .as(AuthorizationGrantType::new)
                .to(builder::authorizationGrantType);
        map.from(properties::getRedirectUri).to(builder::redirectUri);
        map.from(properties::getScope).as(StringUtils::toStringArray).to(builder::scope);
        map.from(properties::getClientName).to(builder::clientName);
        return builder.build();
    }

    private static ClientRegistration.Builder getBuilderFromIssuerIfPossible(String registrationId, String registrationKey, String configuredProviderId, Map<String, OAuth2ClientProperties.Provider> providers) {
        String providerId = (configuredProviderId != null) ? configuredProviderId : registrationKey;
        if (providers.containsKey(providerId)) {
            OAuth2ClientProperties.Provider provider = providers.get(providerId);
            String issuer = provider.getIssuerUri();
            if (issuer != null) {
                ClientRegistration.Builder builder = ClientRegistrations.fromIssuerLocation(issuer).registrationId(registrationId);
                return getBuilder(builder, provider);
            }
        }
        return null;
    }

    private static ClientRegistration.Builder getBuilder(String registrationId, String registrationKey, String configuredProviderId, Map<String, OAuth2ClientProperties.Provider> providers) {
        String providerId = (configuredProviderId != null) ? configuredProviderId : registrationKey;
        CommonOAuth2Provider provider = getCommonProvider(providerId);
        if (provider == null && !providers.containsKey(providerId)) {
            throw new IllegalStateException(getErrorMessage(configuredProviderId, registrationId));
        }
        ClientRegistration.Builder builder = (provider != null) ? provider.getBuilder(registrationId)
                : ClientRegistration.withRegistrationId(registrationId);
        if (providers.containsKey(providerId)) {
            return getBuilder(builder, providers.get(providerId));
        }
        return builder;
    }

    private static String getErrorMessage(String configuredProviderId, String registrationId) {
        return ((configuredProviderId != null) ? "Unknown provider ID '" + configuredProviderId + "'"
                : "Provider ID must be specified for client registration '" + registrationId + "'");
    }

    private static ClientRegistration.Builder getBuilder(ClientRegistration.Builder builder, OAuth2ClientProperties.Provider provider) {
        PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
        map.from(provider::getAuthorizationUri).to(builder::authorizationUri);
        map.from(provider::getTokenUri).to(builder::tokenUri);
        map.from(provider::getUserInfoUri).to(builder::userInfoUri);
        map.from(provider::getUserInfoAuthenticationMethod)
                .as(AuthenticationMethod::new)
                .to(builder::userInfoAuthenticationMethod);
        map.from(provider::getJwkSetUri).to(builder::jwkSetUri);
        map.from(provider::getUserNameAttribute).to(builder::userNameAttributeName);
        return builder;
    }

    private static CommonOAuth2Provider getCommonProvider(String providerId) {
        try {
            return CommonOAuth2Provider.valueOf(providerId.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

}
