package org.edu_sharing.service.authentication;

import com.drew.lang.annotations.NotNull;
import lombok.RequiredArgsConstructor;
import org.edu_sharing.service.authentication.sso.config.ExternalConfigProvider;
import org.edu_sharing.service.authentication.sso.mapping.Mapping;
import org.edu_sharing.service.lti13.sso.config.LTIConfigProvider;
import org.edu_sharing.spring.security.oauth2.config.OAuth2ClientProperties;
import org.edu_sharing.spring.security.oauth2.config.OAuth2ConfigProvider;
import org.edu_sharing.spring.security.saml2.config.Saml2ConfigProvider;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class SSOMappingProvider {

    private final OAuth2ConfigProvider oAuth2ConfigProvider;
    private final Saml2ConfigProvider saml2ConfigProvider;
    private final LTIConfigProvider ltiConfigProvider;
    private final ExternalConfigProvider externalConfigProvider;


    @NotNull
    public Mapping getMapping(Map<String, String> ssoAttributes) {
        String ssoType = ssoAttributes.get(SSOAuthorityMapper.PARAM_SSO_TYPE);
        Mapping mapping = switch (ssoType) {
            case SSOAuthorityMapper.SSO_TYPE_OAUTH -> getOAuthMapping(ssoAttributes);
            case SSOAuthorityMapper.SSO_TYPE_SAML2 -> getSAMLMapping();
            case SSOAuthorityMapper.SSO_TYPE_LTI -> getLtiMapping();
            case SSOAuthorityMapper.SSO_TYPE_EXTERNAL -> getExternalMapping();
            default ->
                    throw new IllegalStateException("Unexpected value: " + ssoType);
        };

        if (mapping == null) {
            return new Mapping();
        }

        return mapping;
    }

    private Mapping getLtiMapping() {
        return ltiConfigProvider.getConfig().getMapping();
    }

    private Mapping getSAMLMapping() {
        return saml2ConfigProvider.getConfig().getMapping();
    }

    private Mapping getExternalMapping() {
        return externalConfigProvider.getConfig().getMapping();
    }

    private Mapping getOAuthMapping(Map<String, String> ssoAttributes) {
        String regContext = ssoAttributes.get(SSOAuthorityMapper.PARAM_SSO_OAUTH_CONTEXT);
        String regKey = ssoAttributes.get(SSOAuthorityMapper.PARAM_SSO_OAUTH_REG_KEY);
        OAuth2ClientProperties config = oAuth2ConfigProvider.getConfig(regContext);
        return config.getMapping().get(regKey);
    }
}
