package org.edu_sharing.service.admin.elastic;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class ElasticExportRequest {
    @Schema(description = "index name, alias or pattern or one of the constants -workspace- / -authorities-. Defaults to the workspace index")
    private String index;
    @Schema(description = "elastic query (json). Either a full search body containing \"query\" or the query object itself. Empty = match_all")
    private String query;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private ElasticExportFormat format;
    @Schema(description = "properties to export (csv/json only). Short names like ccm:title are read from the properties of the document, other values are treated as path in the _source (e.g. nodeRef.id). Use parent::<property> to include parent property values")
    private List<String> properties;
    @Schema(description = "where property values are read from (csv/json only)")
    private ElasticPropertySource propertySource = ElasticPropertySource.elastic;
    @Schema(description = "run in background and store the result in the inbox of the current user")
    private boolean async;
}
