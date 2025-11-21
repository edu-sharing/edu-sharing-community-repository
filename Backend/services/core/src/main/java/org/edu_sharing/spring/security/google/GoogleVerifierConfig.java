package org.edu_sharing.spring.security.google;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.json.JsonFactory;
import lombok.RequiredArgsConstructor;
import org.edu_sharing.spring.security.oauth2.config.OAuth2ClientProperties;
import org.edu_sharing.spring.security.oauth2.config.OAuth2ConfigProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Configuration
public class GoogleVerifierConfig {

    private final OAuth2ConfigProvider configProvider;

    @Bean
    public GoogleIdTokenVerifier googleIdTokenVerifier() throws Exception {


        List<String> googleClientIds = configProvider.getAllConfigs().stream()
                .filter(c -> c.getRegistration().get("google") != null)
                .map(config -> {
                        OAuth2ClientProperties.Registration google = config.getRegistration().get("google");
                    return google.getClientId();
                }).toList();

        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        return new GoogleIdTokenVerifier.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                jsonFactory
        )
                .setIssuer("https://accounts.google.com")
                .setAudience(googleClientIds)
                .build();
    }
}

