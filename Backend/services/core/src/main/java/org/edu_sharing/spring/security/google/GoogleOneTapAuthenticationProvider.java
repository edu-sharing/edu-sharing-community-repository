package org.edu_sharing.spring.security.google;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GoogleOneTapAuthenticationProvider implements AuthenticationProvider {

    private final GoogleIdTokenVerifier verifier;

    public GoogleOneTapAuthenticationProvider(GoogleIdTokenVerifier verifier) {

        this.verifier = verifier;
    }

    @Override
    public Authentication authenticate(Authentication authentication) {

        GoogleOneTapAuthenticationToken token = (GoogleOneTapAuthenticationToken) authentication;
        String credential = (String) token.getCredentials();

        try {
            GoogleIdToken idToken = verifier.verify(credential);

            if (idToken == null) {
                throw new BadCredentialsException("Invalid Google ID token");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();

            // Load or auto-create user
            //UserDetails user = userDetailsService.loadUserByUsername(email);
            //@TODO user verification
            boolean emailVerified = Boolean.TRUE.equals(payload.getEmailVerified());

            if (!emailVerified) {
                throw new BadCredentialsException("Email not verified");
            }
            Map<String, Object> attributes = new HashMap<>(payload.getUnknownKeys());
            attributes.put("email", email);
            attributes.put("sub", payload.getSubject());
            DefaultOAuth2User user = new DefaultOAuth2User(List.of(),attributes,"email");

            return new GoogleOneTapAuthenticationToken(
                    user,
                    user.getAuthorities()
            );

        } catch (Exception e) {
            throw new BadCredentialsException("Failed to validate Google token", e);
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return GoogleOneTapAuthenticationToken.class.isAssignableFrom(authentication);
    }
}

