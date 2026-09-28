package org.edu_sharing.alfresco.service.config.model;

import com.typesafe.config.Optional;
import io.swagger.v3.oas.annotations.media.Schema;
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
    /**
     * <b>This is the global value.</b> {@code repository.guest} can be overridden
     * per context ({@code repository.context.<id>.repository.guest}), but the whole
     * bean is built from the root config
     */
    @Data
    public static class RepositoryConfigBackend {
        private ChildobjectsConfig childobjects;
        private WebComponentsConfig webComponents;

        @Optional
        private GuestConfig guest;
    }
    @Data
    public static class GuestConfig {
        @Optional
        @Schema(description = "repository.guest.enabled: whether anonymous visitors get a guest session. When false, the Frontend-Server renders the shell without any node metadata")
        private Boolean enabled;
    }
    @Data
    public static class WebComponentsConfig {
        private boolean enabled;
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
