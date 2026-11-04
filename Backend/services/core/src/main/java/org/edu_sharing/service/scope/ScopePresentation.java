package org.edu_sharing.service.scope;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * Presentation settings of a scope (ccm:scope_settings, json). Extensible without a model change: unknown keys are
 * ignored when reading.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "ScopePresentation")
public class ScopePresentation implements Serializable {
    /**
     * properties shown on the content cards (search, collection contents) inside the scope, in this order.
     * Null or empty: the columns of the mds list
     */
    @Schema(description = "properties of the content cards (search, collection contents), null or empty: the mds default")
    private List<String> cardColumns;
    /**
     * properties the lists inside the scope can be sorted by (editorial page and search), in this order, "score" for
     * the relevance. Null or empty: the sorts of the mds
     */
    @Schema(description = "sortable properties inside the scope (editorial page, search), \"score\" for the relevance, null or empty: the mds default")
    private List<String> sortColumns;
}
