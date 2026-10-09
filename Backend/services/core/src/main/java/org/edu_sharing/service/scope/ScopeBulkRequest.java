package org.edu_sharing.service.scope;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Bulk change of assignments. Either nodeIds or search is set. The search is resolved inside the job with the
 * permissions of the user who started it.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScopeBulkRequest implements Serializable {
    private List<String> nodeIds;
    private ScopeBaseQuery.Query search;
    /** properties to set (ignored if delete is true) */
    private Map<String, List<String>> properties;
    /** true: remove the assignments instead of setting properties */
    private boolean delete;
    private String reason;
}
