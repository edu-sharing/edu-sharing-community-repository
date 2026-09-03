package org.edu_sharing.spring.security.server.oauth2.persistence;

import org.edu_sharing.spring.security.basic.EduSharingPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import tools.jackson.databind.json.JsonMapper;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Guards the polymorphic type validator of the mapper the authorization is persisted with. Without the
 * types named there, a consent flow fails on the way back out of oauth2_authorization with
 * "Could not resolve type id ... denied resolution", and the user never gets an authorization code.
 */
class AlfrescoOAuth2AuthorizationServiceTest {

    /**
     * the key spring authorization server stores the authentication of the user under
     */
    private static final String PRINCIPAL_ATTRIBUTE = java.security.Principal.class.getName();

    @Test
    @DisplayName("the authentication AcegiBackedSecurityContextHolderStrategy builds survives a round trip")
    void serializesEduSharingPrincipal() {
        Collection<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("GROUP_EVERYONE"));
        // the alfresco backed principal: hashIndicator and salt come straight off RepositoryAuthenticatedUser
        EduSharingPrincipal principal =
                new EduSharingPrincipal("admin", authorities, List.of("md4"), "0123456789abcdef");

        UsernamePasswordAuthenticationToken token = roundTrip(principal, authorities);

        EduSharingPrincipal restored = assertInstanceOf(EduSharingPrincipal.class, token.getPrincipal());
        assertEquals("admin", restored.getUsername());
        assertEquals(List.of("ROLE_USER", "GROUP_EVERYONE"),
                restored.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList());
        assertEquals(List.of("md4"), restored.getHashIndicator());
        assertEquals("0123456789abcdef", restored.getSalt());
        // the strategy puts the principal into the details as well
        assertInstanceOf(EduSharingPrincipal.class, token.getDetails());
    }

    @DisplayName("the concrete collection type of the alfresco fields does not matter")
    @Test
    void serializesMutableCollections() {
        Collection<GrantedAuthority> authorities = new ArrayList<>(List.of(new SimpleGrantedAuthority("ROLE_USER")));
        EduSharingPrincipal principal =
                new EduSharingPrincipal("admin", authorities, new ArrayList<>(List.of("md4")), "salt");

        UsernamePasswordAuthenticationToken token = roundTrip(principal, authorities);

        assertEquals("admin", ((EduSharingPrincipal) token.getPrincipal()).getUsername());
    }

    @DisplayName("an alfresco user without a stored password hash has neither hashIndicator nor salt")
    @Test
    void serializesWithoutHashIndicatorAndSalt() {
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        EduSharingPrincipal principal = new EduSharingPrincipal("admin", authorities, null, (Serializable) null);

        UsernamePasswordAuthenticationToken token = roundTrip(principal, authorities);

        assertEquals("admin", ((EduSharingPrincipal) token.getPrincipal()).getUsername());
    }

    /**
     * writes the attribute map the way JdbcOAuth2AuthorizationService does and reads it back
     */
    private UsernamePasswordAuthenticationToken roundTrip(EduSharingPrincipal principal,
                                                          Collection<GrantedAuthority> authorities) {
        UsernamePasswordAuthenticationToken token =
                UsernamePasswordAuthenticationToken.authenticated(principal, "credentials", authorities);
        token.setDetails(principal);

        // a LinkedHashMap like the one OAuth2Authorization holds - a final map type would carry no type
        // id and would not exercise the validator at all
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put(PRINCIPAL_ATTRIBUTE, token);

        JsonMapper jsonMapper = AlfrescoOAuth2AuthorizationService.createJsonMapper();
        String json = jsonMapper.writeValueAsString(attributes);
        Map<String, Object> restored = jsonMapper.readValue(json,
                jsonMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class));
        return assertInstanceOf(UsernamePasswordAuthenticationToken.class, restored.get(PRINCIPAL_ATTRIBUTE));
    }
}
