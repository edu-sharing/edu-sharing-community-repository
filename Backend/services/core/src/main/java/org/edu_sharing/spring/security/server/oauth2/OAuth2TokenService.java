package org.edu_sharing.spring.security.server.oauth2;

import lombok.extern.slf4j.Slf4j;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.config.ConfigService;
import org.edu_sharing.spring.conditions.ConditionalOnProperty;
import org.edu_sharing.spring.security.server.oauth2.config.OAuth2ConfigService;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;

import java.net.URISyntaxException;

@Slf4j
@Service
@ConditionalOnProperty(name = OAuth2ConfigService.CONFIG_PATH + ".enabled", havingValue = "true")
public class OAuth2TokenService {

    ConfigService configService;

    public OAuth2TokenService(ConfigService configService) {
        log.info("Initializing OAuth2TokenService");
        this.configService = configService;
    }

    public String extractUsername(String token) throws URISyntaxException {
        JwtDecoder jwtDecoder = NimbusJwtDecoder
                .withJwkSetUri(ApplicationInfoList.getHomeRepository().getBaseUrl()+ "/edu-sharing/oauth2server/jwks")
                .build();
        Jwt jwt = jwtDecoder.decode(token);
        return jwt.getSubject();
    }
}
