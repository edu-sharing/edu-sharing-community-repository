package org.edu_sharing.spring.security.server.oauth2;

import org.edu_sharing.spring.security.server.oauth2.config.OAuth2Config;
import org.edu_sharing.spring.security.server.oauth2.config.OAuth2ConfigService;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Covers how the entries of {@code security.authentication.oauth2.trustedIssuers} are grouped and how a
 * decoded token is matched against them. The decoder itself is not exercised - it would fetch a key set.
 */
class TrustedIssuerRegistryTest {

    private static final String ISSUER = "https://keycloak.example.org/realms/edu";

    @Test
    void picksTheEntryOfTheClientTheTokenWasIssuedTo() {
        TrustedIssuerRegistry registry = registry(
                entry(ISSUER, "edu-sharing-api", "provisioning-app", "provisioning-user"),
                entry(ISSUER, "edu-sharing-api", "reporting-app", "reporting-user"));

        assertEquals("reporting-user", resolve(registry, token("edu-sharing-api", "reporting-app")));
        assertEquals("provisioning-user", resolve(registry, token("edu-sharing-api", "provisioning-app")));
    }

    @Test
    void tellsEntriesApartByAudienceAlone() {
        TrustedIssuerRegistry registry = registry(
                entry(ISSUER, "edu-sharing-api", "", "api-user"),
                entry(ISSUER, "edu-sharing-import", "", "import-user"));

        assertEquals("import-user", resolve(registry, token("edu-sharing-import", null)));
    }

    @Test
    void fallsBackToTheClaimWhenNoFixedUserIsConfigured() {
        OAuth2Config.TrustedIssuer entry = entry(ISSUER, "edu-sharing-api", "", "");
        entry.setUsernameClaim("preferred_username");
        TrustedIssuerRegistry registry = registry(entry);

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("iss", ISSUER)
                .audience(Collections.singletonList("edu-sharing-api"))
                .claim("preferred_username", "alice")
                .subject("f1e2d3c4")
                .build();

        assertEquals("alice", resolve(registry, jwt));
    }

    @Test
    void rejectsATokenThatFitsNoneOfTheEntries() {
        TrustedIssuerRegistry registry = registry(
                entry(ISSUER, "edu-sharing-api", "provisioning-app", "provisioning-user"),
                entry(ISSUER, "edu-sharing-api", "reporting-app", "reporting-user"));

        assertNull(registry.get(ISSUER).match(token("edu-sharing-api", "some-other-app")));
    }

    @Test
    void anIssuerThatIsNotConfiguredHasNoGroup() {
        TrustedIssuerRegistry registry = registry(entry(ISSUER, "edu-sharing-api", "", "api-user"));

        assertNull(registry.get("https://other.example.org/realms/edu"));
        assertNull(registry.get(null));
    }

    @Test
    void refusesTwoEntriesOfOneIssuerThatCannotBeToldApart() {
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> registry(
                entry(ISSUER, "edu-sharing-api", "provisioning-app", "provisioning-user"),
                entry(ISSUER, "edu-sharing-api", "provisioning-app", "someone-else")));

        assertTrue(e.getMessage().contains("could never be reached"));
    }

    @Test
    void refusesEntriesOfOneIssuerWithDifferentKeySets() {
        OAuth2Config.TrustedIssuer second = entry(ISSUER, "edu-sharing-import", "", "import-user");
        second.setJwkSetUri("https://keycloak.example.org/realms/other/protocol/openid-connect/certs");

        IllegalStateException e = assertThrows(IllegalStateException.class, () -> registry(
                entry(ISSUER, "edu-sharing-api", "", "api-user"), second));

        assertTrue(e.getMessage().contains("same key set"));
    }

    @Test
    void refusesAnEntryThatBindsATokenToNothing() {
        assertThrows(IllegalStateException.class, () -> registry(entry(ISSUER, "", "", "api-user")));
    }

    private String resolve(TrustedIssuerRegistry registry, Jwt jwt) {
        TrustedIssuerRegistry.TrustedIssuerGroup group = registry.get(ISSUER);
        assertNotNull(group);
        TrustedIssuerRegistry.TrustedIssuerContext context = group.match(jwt);
        assertNotNull(context);
        return context.resolveUsername(jwt);
    }

    private static Jwt token(String audience, String authorizedParty) {
        Jwt.Builder builder = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("iss", ISSUER)
                .audience(Collections.singletonList(audience))
                .subject("f1e2d3c4");
        if (authorizedParty != null) {
            builder.claim("azp", authorizedParty);
        }
        return builder.build();
    }

    private static OAuth2Config.TrustedIssuer entry(String issuerUri, String audience, String authorizedParty,
                                                    String username) {
        OAuth2Config.TrustedIssuer entry = new OAuth2Config.TrustedIssuer();
        entry.setIssuerUri(issuerUri);
        entry.setAudience(audience);
        entry.setAuthorizedParty(authorizedParty);
        entry.setUsername(username);
        return entry;
    }

    private static TrustedIssuerRegistry registry(OAuth2Config.TrustedIssuer... entries) {
        List<OAuth2Config.TrustedIssuer> configured = Arrays.asList(entries);
        OAuth2Config config = new OAuth2Config();
        config.setTrustedIssuers(configured);
        OAuth2ConfigService configService = mock(OAuth2ConfigService.class);
        when(configService.getDefaultConfig()).thenReturn(config);
        return new TrustedIssuerRegistry(configService);
    }
}
