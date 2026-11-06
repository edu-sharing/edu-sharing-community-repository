package org.edu_sharing.alfresco.service.config.model;

import com.typesafe.config.Optional;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * {@link ValuesBackend} plus the lightbend config the Frontend-Server needs to deliver
 * the angular shell
 * <p>
 * Only handed out on the internal port ({@code ApplicationInfoList.isInternalPortRequest}),
 * where the Frontend-Server fetches the config - documented in the openapi spec, but not
 * served to public callers.
 * <p>
 * Every addition is {@link Optional}: a custom config replacing one of these blocks as
 * a whole must not break the config endpoint.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "ValuesBackend plus the config the Frontend-Server needs to deliver the angular shell. Only returned on the internal port")
public class ValuesBackendInternal extends ValuesBackend {
    @Optional
    private AngularConfig angular;
    @Optional
    private ExporterConfig exporter;
    /**
     * not lightbend config: set by ConfigServiceImpl from SearchServiceElastic.WORKSPACE_INDEX,
     * so the Frontend-Server follows the index of the release without its own configuration
     */
    @Optional
    @Schema(description = "name of the elasticsearch workspace index (SearchServiceElastic.WORKSPACE_INDEX), the Frontend-Server reads node metadata from it")
    private String elasticWorkspaceIndex;

    @Data
    public static class AngularConfig {
        /** angular.headers, as SecurityHeadersFilter reads them */
        @Optional
        @Schema(description = "angular.headers: security headers of the shell pages; Content-Security-Policy is a map directive -> value, {{ngCspNonce}} is replaced per page")
        private Map<String, Object> headers;
        /** html snippet added to the head of every shell page, may use {{ngCspNonce}} */
        @Optional
        @Schema(description = "angular.customHtmlHeaders: html added to the head of every shell page, {{ngCspNonce}} is replaced per page")
        private String customHtmlHeaders;
    }

    @Data
    public static class ExporterConfig {
        @Optional
        @Schema(description = "exporter.lrmi: the mapping LRMITool uses")
        private LrmiConfig lrmi;
    }

    /**
     * mapping used by LRMITool, so the Frontend-Server renders the LRMI JSON-LD of the
     * render page from the node metadata alone
     */
    @Data
    public static class LrmiConfig {
        @Optional
        @Schema(description = "put the LRMI JSON-LD into the head of the render page (Frontend-Server), default true")
        private Boolean enrichment;
        // lrmi field -> "prop1,prop2" (first filled wins) / "prop1+prop2" (combined), short names
        @Optional
        // Map<String, Object> for ConfigBeanFactory, the values are strings
        @Schema(description = "lrmi field -> property expression, short names: \"a,b\" first filled wins, \"a+b\" values combined", additionalPropertiesSchema = String.class)
        private Map<String, Object> properties;
        // lrmi field -> vcard property (short name)
        @Optional
        @Schema(description = "lrmi field -> vcard property (short name), rendered as schema.org Person / Organization", additionalPropertiesSchema = String.class)
        private Map<String, Object> vcard;
    }
}
