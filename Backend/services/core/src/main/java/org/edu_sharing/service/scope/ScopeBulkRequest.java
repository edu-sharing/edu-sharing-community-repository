package org.edu_sharing.service.scope;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    /**
     * properties of {@link #properties} whose values are added to the existing ones instead of replacing them (multi
     * value properties only, duplicates are skipped)
     */
    private Set<String> appendProperties;
    /**
     * values to remove from the existing ones per property, applied before {@link #properties} (e.g. values of a
     * selection that were deselected in the bulk editor)
     */
    private Map<String, List<String>> removeValues;
    /** true: remove the assignments instead of setting properties */
    private boolean delete;
    private String reason;
}
