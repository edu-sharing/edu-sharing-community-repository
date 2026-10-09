package org.edu_sharing.service.scope;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Entry of a node in a scope (mongo collection scope_assignment). The original node is never modified.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScopeAssignment implements Serializable {
    private String scopeId;
    /** uuid of the original node (or the ccm:remoteobject shadow) */
    private String nodeId;
    /** local property name (e.g. "ccm:editorial_release") -> values. single value properties carry one value */
    private Map<String, List<String>> properties;
    /** derived: a property has one of the publishedValues of its field (see {@link ScopeField}) */
    private boolean published;
    /** derived: a property has one of the excludedValues of its field (see {@link ScopeField}) */
    private boolean excluded;
    private ScopeSource source;
    private Date created;
    private String createdBy;
    private Date modified;
    private String modifiedBy;
}
