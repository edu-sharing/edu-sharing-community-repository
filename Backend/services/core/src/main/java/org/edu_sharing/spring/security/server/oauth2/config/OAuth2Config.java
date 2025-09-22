package org.edu_sharing.spring.security.server.oauth2.config;

import lombok.Data;

import java.util.List;

@Data
public class OAuth2Config {

    boolean enabled;
    List<Client> clients;

    @Data
    public static final class Client {
        String clientId;
        String clientSecret;
        String clientAuthenticationMethod;
        List<String> authorizationGrantTypes;
        String redirectUri;
        List<String> scopes;
    }
}
