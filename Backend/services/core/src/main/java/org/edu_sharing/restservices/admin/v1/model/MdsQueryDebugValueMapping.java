package org.edu_sharing.restservices.admin.v1.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class MdsQueryDebugValueMapping {
    private String value;
    @Schema(description = "key of the mds statement which was used for this value (the value itself for value-specific statements, null for the default statement)")
    private String statementKey;
    @Schema(description = "statement template of the mds for this value (common query variables like ${user.*} are already replaced)")
    private String statement;
    @Schema(description = "resulting elastic query fragment for this single value")
    private String elasticQuery;
}
