package org.edu_sharing.spring.security.openid.config;

import lombok.Data;

@Data
public class OpenIdConfig {
   String issuer;
   String clientId;
   String secret;
   String contextId;
}
