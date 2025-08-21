package org.edu_sharing.alfresco.service.config.model;

import lombok.Data;

import java.util.Map;

/**
 * this class represents all lightbend based configs which can and shall be exposed to the angular via the config
 * Note: This data is exposed to all users
 */
@Data
public class ValuesBackend {
    private SecurityConfig security;
    @Data
    public static class SecurityConfig {
      private Access access;
    }
    @Data
    public static class Access {
        // private String openapi;
        private Map<String, Object> endpoints;
    }
}
