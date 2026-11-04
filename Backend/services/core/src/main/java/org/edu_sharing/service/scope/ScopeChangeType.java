package org.edu_sharing.service.scope;

public enum ScopeChangeType {
    /** assignments of the nodes were created or changed */
    UPSERT,
    /** assignments of the nodes were removed */
    DELETE,
    /** the scope group was deleted, all its entries are gone (nodeIds is empty) */
    SCOPE_DELETED
}
