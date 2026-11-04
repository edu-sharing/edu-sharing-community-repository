package org.edu_sharing.service.scope;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Candidate queries of a scope (ccm:scope_base_query). The queries are OR-combined, evaluated live at search
 * time, always restricted to GROUP_EVERYONE and only visible for editors.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScopeBaseQuery implements Serializable {
    /** increased on every change, referenced by the history */
    private long version;
    private List<Query> queries;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    // explicit name: the simple name "Query" is already used by another api schema
    @Schema(name = "ScopeQuery")
    public static class Query implements Serializable {
        private String mds;
        private String query;
        private Map<String, List<String>> criteria;
    }
}
