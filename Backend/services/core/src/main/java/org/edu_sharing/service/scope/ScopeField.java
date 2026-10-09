package org.edu_sharing.service.scope;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * One entry of the field list of a scope group (ccm:scope_fields).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScopeField implements Serializable {
    /** property in local form, e.g. "ccm:editorial_release" */
    private String id;
    private ScopeFieldVisibility visibility;
    /** true: the value replaces the original value of the node inside the own active scope */
    private boolean replaces;
    /**
     * values of this property that mark the entry as published (e.g. ["zugelassen"] for ccm:editorial_release).
     * the values themselves come from the valuespace of the scope mds, they are never hardcoded
     */
    private List<String> publishedValues;
    /** values of this property that mark the entry as excluded (e.g. ["abgelehnt"] for ccm:editorial_inventory) */
    private List<String> excludedValues;
    /**
     * values set when content is uploaded into the shared folder of the scope group (e.g. ["accepted"] for
     * ccm:editorial_inventory). Fields without uploadValues stay empty on upload
     */
    private List<String> uploadValues;
}
