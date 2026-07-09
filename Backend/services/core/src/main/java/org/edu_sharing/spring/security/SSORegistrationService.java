package org.edu_sharing.spring.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.edu_sharing.spring.security.oauth2.OAuth2ClientPropertiesMapper;
import org.edu_sharing.spring.security.oauth2.config.OAuth2ConfigProvider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SSORegistrationService {

    private final OAuth2ConfigProvider oAuth2ConfigProvider;

    public List<ClientRegistration> getClientRegistrations() {
        return new ArrayList<>(
                new OAuth2ClientPropertiesMapper(oAuth2ConfigProvider).asClientRegistrations().values());
    }
}
