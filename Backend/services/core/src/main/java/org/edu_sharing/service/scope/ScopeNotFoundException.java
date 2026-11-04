package org.edu_sharing.service.scope;

/**
 * unknown scope (no group with groupType SCOPE), entry or bulk job. REST: 404
 */
public class ScopeNotFoundException extends RuntimeException {
    public ScopeNotFoundException(String message) {
        super(message);
    }
}
