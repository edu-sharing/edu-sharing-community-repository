package org.edu_sharing.restservices.admin.v1.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.edu_sharing.restservices.shared.Node;
import org.edu_sharing.restservices.shared.SearchResultElastic;

import java.util.ArrayList;
import java.util.List;

@Data
public class MdsQueryDebugResult {
    private String metadataset;
    private String query;
    @Schema(description = "join of the parameters of the query (AND / OR)")
    private String join;
    @Schema(description = "all parameters which are defined in the mds query")
    private List<String> availableParameters = new ArrayList<>();
    private List<MdsQueryDebugCriterion> criteria = new ArrayList<>();
    private boolean globalConditionsIncluded;
    private List<String> warnings = new ArrayList<>();
    @Schema(description = "the generated elastic search body (wrapper queries are decoded, can be used directly in the elastic dsl debugger)")
    private String dsl;
    @Schema(description = "separate aggregations request for facets which exclude their own values (if required)")
    private String aggregationsDsl;
    @Schema(description = "elastic index the dsl is meant for")
    private String index;
    @Schema(description = "result of the dsl, only if execute was requested")
    private SearchResultElastic<Node> result;
}
