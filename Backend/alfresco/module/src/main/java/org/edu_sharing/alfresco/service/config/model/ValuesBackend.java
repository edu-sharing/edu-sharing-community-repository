package org.edu_sharing.alfresco.service.config.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * this class represents all lightbend based configs which can and shall be exposed to the angular via the config
 * Note: This data is exposed to all users
 */
@Data
public class ValuesBackend {
    private SecurityConfig security;
    private RepositoryConfigBackend repository;
    @Data
    public static class RepositoryConfigBackend {
        private ChildobjectsConfig childobjects;
    }
    @Data
    public static class ChildobjectsConfig {
        private List<String> ignoredInheritMetadata;
    }
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
