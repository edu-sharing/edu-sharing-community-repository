package org.edu_sharing.service.scope;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * Definition of a scope, read from the properties of its scope group (aspect ccm:scopeGroup).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScopeDefinition implements Serializable {
    /** authority name of the scope group, e.g. "GROUP_ORG_wlo" */
    private String scopeId;
    private String displayName;
    /** ccm:scope_mds */
    private String mds;
    /** ccm:scope_fields */
    private List<ScopeField> fields;
    /** ccm:scope_base_query, null if none is set */
    private ScopeBaseQuery baseQuery;
    /** ccm:scope_folder (node id) */
    private String folderId;
    /** ccm:scope_settings, null if none is set */
    private ScopePresentation presentation;
}
