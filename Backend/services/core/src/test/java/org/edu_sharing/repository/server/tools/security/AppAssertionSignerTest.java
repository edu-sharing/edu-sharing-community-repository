package org.edu_sharing.repository.server.tools.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.stream.StreamSupport;

import static org.edu_sharing.repository.server.tools.security.SigningTestVector.*;
import static org.junit.jupiter.api.Assertions.*;

class AppAssertionSignerTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String RENDERING_AUDIENCE = "rendering-service";

    private final AppAssertionSigner underTest = new AppAssertionSigner(signer("SHA512withRSA"));

    private static JsonNode part(String jws, int index) throws Exception {
        return JSON.readTree(Base64.getUrlDecoder().decode(jws.split("\\.")[index]));
    }

    private static boolean signatureValid(String jws, String algorithm) throws Exception {
        String[] parts = jws.split("\\.");
        java.security.Signature signature = java.security.Signature.getInstance(algorithm);
        signature.initVerify(publicKey());
        signature.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII));
        return signature.verify(Base64.getUrlDecoder().decode(parts[2]));
    }

    @Test
    void headerAndClaimsAreExactlyWhatTheProtocolSays() throws Exception {
        // Arrange & Act
        String token = underTest.sign(RENDERING_AUDIENCE, IAT, JTI);

        // Assert
        JsonNode header = part(token, 0);
        assertEquals(List.of("alg", "typ", "kid"), StreamSupport.stream(java.util.Spliterators.spliteratorUnknownSize(header.fieldNames(), 0), false).toList());
        assertEquals("RS512", header.get("alg").asText());
        assertEquals("edu-app+jwt", header.get("typ").asText());
        assertEquals(KID, header.get("kid").asText());
        JsonNode claims = part(token, 1);
        assertEquals(APP_ID, claims.get("iss").asText());
        assertEquals(RENDERING_AUDIENCE, claims.get("aud").asText());
        assertEquals(IAT, claims.get("iat").asLong());
        assertEquals(IAT + 30, claims.get("exp").asLong());
        assertEquals(JTI, claims.get("jti").asText());
        assertEquals(5, claims.size(), "no user, no method or path: it is the repository itself, for read-only calls");
        assertTrue(signatureValid(token, "SHA512withRSA"));
    }

    @Test
    void signsNowWithANewJtiEveryTimeAndNamesTheAlgorithmOfTheSigner() throws Exception {
        // Arrange
        AppAssertionSigner sha256 = new AppAssertionSigner(signer("SHA256withRSA"));

        // Act
        String first = sha256.sign(RENDERING_AUDIENCE);
        String second = sha256.sign(RENDERING_AUDIENCE);

        // Assert
        assertEquals("RS256", part(first, 0).get("alg").asText());
        assertTrue(signatureValid(first, "SHA256withRSA"));
        assertTrue(Math.abs(System.currentTimeMillis() / 1000 - part(first, 1).get("iat").asLong()) < 10, "signed now");
        assertNotEquals(part(first, 1).get("jti").asText(), part(second, 1).get("jti").asText());
        assertEquals(22, part(first, 1).get("jti").asText().length());
    }

    @Test
    void aChangedPartBreaksTheSignature() throws Exception {
        // Arrange
        String[] parts = underTest.sign(RENDERING_AUDIENCE, IAT, JTI).split("\\.");
        String otherClaims = Base64.getUrlEncoder().withoutPadding().encodeToString(
                new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8).replace(APP_ID, "mallory").getBytes(StandardCharsets.UTF_8));

        // Act & Assert
        assertFalse(signatureValid(parts[0] + "." + otherClaims + "." + parts[2], "SHA512withRSA"));
    }

    @Test
    void anAlgorithmWithoutAJwsNameIsRefused() {
        // Arrange
        AppAssertionSigner sha1 = new AppAssertionSigner(signer("SHA1withRSA"));

        // Act & Assert
        assertThrows(HomeRepositorySigner.SigningFailedException.class, () -> sha1.sign(RENDERING_AUDIENCE));
    }
}
