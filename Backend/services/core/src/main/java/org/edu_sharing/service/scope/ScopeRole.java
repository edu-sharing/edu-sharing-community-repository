package org.edu_sharing.service.scope;

/**
 * Role of the current user inside the active scope (see concept §5.1).
 */
public enum ScopeRole {
    /** member of the scope group */
    EDITOR,
    /** guest user of the context that references the scope group */
    GUEST,
    /** signed in via the domain of a context, but not a member of the scope group (read only) */
    TEMPORARY_READER
}
