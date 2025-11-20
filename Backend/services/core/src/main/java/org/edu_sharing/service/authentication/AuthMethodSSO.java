/**
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */
package org.edu_sharing.service.authentication;

import com.typesafe.config.Config;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.repo.security.authentication.AuthenticationException;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.spring.security.google.SecurityConfigGoogleOneTap;
import org.edu_sharing.spring.security.oauth2.SecurityConfigurationOAuth2;
import org.springframework.stereotype.Component;

import java.util.Map;


/**
 * ensure that the authentication method of this class isn't provided through an webservice or something
 *
 * @author rudi
 *
 */
@Slf4j
@RequiredArgsConstructor
@Component("ccAuthMethodSSO")
public class AuthMethodSSO implements AuthMethodInterface {
    private final SSOAuthorityMapper ssoAuthorityMapper;
    private final LightbendConfigLoader configLoader;


    @Override
    public String authenticate(Map<String, String> params) throws AuthenticationException {
        //for security reasons: sso auth should be explicit activated in config
        //so that we don't get an open gate when someone forgets to remove the shibboleth servlet from web.xml and don't protects this url
        //(shibboleth header usage)

        String paramAuthType = params.get(SSOAuthorityMapper.PARAM_SSO_TYPE);

        if (paramAuthType == null) {
            log.error("missing paramAuthType");
            throw new AuthenticationException(AuthenticationExceptionMessages.MISSING_PARAM);
        }

        Config config = configLoader.getConfig();
        String springProfiles = config.hasPath("spring.profiles.active")
                ? config.getString("spring.profiles.active")
                : null;
        if (SSOAuthorityMapper.isShibbolethAuthType(paramAuthType)
                && springProfiles != null
                && (springProfiles.contains(SecurityConfigurationOAuth2.PROFILE_ID) || springProfiles.contains("samlEnabled") || springProfiles.contains(SecurityConfigGoogleOneTap.PROFILE_ID))) {
            return ssoAuthorityMapper.mapAuthority(params);
        } else if (SSOAuthorityMapper.isShibbolethAuthType(paramAuthType)
                && config.getBoolean("security.sso.external.enabled")) {
            return ssoAuthorityMapper.mapAuthority(params);
        } else if (SSOAuthorityMapper.SSO_TYPE_LTI.equals(paramAuthType) &&
                config.getBoolean("security.sso.lti.enabled")) {
            return ssoAuthorityMapper.mapAuthority(params);
        } else {
            log.error("{} no SSO(shibboleth) auth configured. authType:{}", AuthenticationExceptionMessages.INVALID_AUTHENTICATION_METHOD, paramAuthType);
            throw new AuthenticationException(AuthenticationExceptionMessages.INVALID_AUTHENTICATION_METHOD);
        }
    }


}
