package org.edu_sharing.service.scope;

public class ScopesDisabledException extends RuntimeException {
    public ScopesDisabledException() {
        super("scopes are disabled (repository.scopes.enabled)");
    }
}
