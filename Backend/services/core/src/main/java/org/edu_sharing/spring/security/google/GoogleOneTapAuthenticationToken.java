package org.edu_sharing.spring.security.google;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class GoogleOneTapAuthenticationToken extends AbstractAuthenticationToken {

    private final Object principal;
    private final String credential;

    // Constructor for authentication request (unauthenticated)
    public GoogleOneTapAuthenticationToken(String credential) {
        super(null);
        this.principal = null;
        this.credential = credential;
        setAuthenticated(false);
    }

    // Constructor for successful authentication
    public GoogleOneTapAuthenticationToken(Object principal,
                                           Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        this.credential = null;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return credential;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }
}

