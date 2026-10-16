package org.edu_sharing.restservices.admin.v1.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class MdsQueryDebugCriterion {
    private String property;
    private List<String> values = new ArrayList<>();
    @Schema(description = "true if the parameter is defined in the mds query")
    private boolean known;
    @Schema(description = "true if the criterion is part of the generated dsl")
    private boolean applied;
    @Schema(description = "parameter is applied as filter (true) or as scoring query (false)")
    private boolean asFilter;
    private boolean multiple;
    private String multipleJoin;
    private boolean exactMatching;
    private boolean mandatory;
    @Schema(description = "> 0 if this criterion is removed when the search has no results")
    private int ignorable;
    private String preprocessor;
    @Schema(description = "all statements of the parameter as defined in the mds (key \"default\" for the statement without value attribute)")
    private Map<String, String> statements;
    private List<MdsQueryDebugValueMapping> valueMappings = new ArrayList<>();
    @Schema(description = "resulting elastic query fragment for all values of this criterion")
    private String elasticQuery;
    private List<String> warnings = new ArrayList<>();
}
