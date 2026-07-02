package org.edu_sharing.service.authentication;

import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.repo.security.authentication.AuthenticationException;
import org.apache.tika.utils.StringUtils;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.springframework.stereotype.Component;

@Slf4j
@Component("eduAuthMethodTrustedApplication")
@RequiredArgsConstructor
@Setter
public class AuthMethodTrustedApplication implements AuthMethodInterface {

	private SSOAuthorityMapper ssoAuthorityMapper;

    public String authenticate(Map<String, String> params) throws AuthenticationException {

        String userName = params.get(ssoAuthorityMapper.getSSOUsernameProp(params));

        String applicationId = params.get(SSOAuthorityMapper.PARAM_AUTHBYAPP_APP_ID);
        String clientIp = params.get(SSOAuthorityMapper.PARAM_AUTHBYAPP_APP_IP);

        // check params
		if(StringUtils.isBlank(applicationId) || StringUtils.isBlank(userName)){
			log.error(AuthenticationExceptionMessages.MISSING_PARAM);
            log.error(" username:{} applicationId:{} ( clientIp:{})", userName, applicationId, clientIp);
            throw new AuthenticationException(AuthenticationExceptionMessages.MISSING_PARAM);
        }

        // check applicationId
        final ApplicationInfo appInfo = ApplicationInfoList.getRepositoryInfoById(applicationId);
        if (appInfo == null || appInfo.getTrustedclient() == null || !appInfo.getTrustedclient().equals("true")) {
            log.info("{} {}", AuthenticationExceptionMessages.INVALID_APPLICATION, appInfo);
            throw new AuthenticationException(AuthenticationExceptionMessages.INVALID_APPLICATION);
        }

        // check host
        if (ssoAuthorityMapper.isAuthByAppCheckClientIp() && (clientIp == null || !appInfo.isTrustedHost(clientIp))) {
            log.error("{} clientHost:{} appInfo.trusted hosts:{} {} {} appInfo.getAppId():{} appfile:{} param appid:{}", AuthenticationExceptionMessages.INVALID_HOST, clientIp, appInfo.getHost(), appInfo.getHostAliases(), appInfo.getDomain(), appInfo.getAppId(), appInfo.getAppFile(), applicationId);
            throw new AuthenticationException(AuthenticationExceptionMessages.INVALID_HOST + ": " + clientIp);
        }

        params.put(SSOAuthorityMapper.PARAM_SSO_TYPE, SSOAuthorityMapper.SSO_TYPE_AuthByApp);
        return ssoAuthorityMapper.mapAuthority(params);
    }

}
