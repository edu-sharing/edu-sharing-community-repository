package org.edu_sharing.service.scope;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Index fragment of a node, computed by the repository and written as-is by the tracker (ADR-2).
 * Child objects carry the fragment of their parent.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScopeIndexData implements Serializable {
    private String nodeId;
    /** es: scope_ids - scopes with an entry */
    private List<String> scopeIds;
    /** es: scope_ids_published */
    private List<String> scopeIdsPublished;
    /** es: scope_ids_excluded */
    private List<String> scopeIdsExcluded;
    /** es: scope_overrides - "&lt;scopeId&gt;|&lt;property&gt;" for every set property with replaces = true (marker only, no values) */
    private List<String> scopeOverrides;
    /** es: scopes (nested) */
    private List<Entry> scopes;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    // explicit name: the simple name "Entry" is already used by another api schema
    @Schema(name = "ScopeIndexEntry")
    public static class Entry implements Serializable {
        private String scopeId;
        private boolean published;
        private boolean excluded;
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
        private Date created;
        private String createdBy;
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
        private Date modified;
        private String modifiedBy;
        /** only values that are actually set, no copies of the original */
        private Map<String, List<String>> properties;
        /**
         * properties of the field list with visibility public that do not replace an original value: only these may
         * be searched and shown outside of the own scope, and only if the entry is published (concept §5.4)
         */
        private List<String> publicFields;
    }
}
