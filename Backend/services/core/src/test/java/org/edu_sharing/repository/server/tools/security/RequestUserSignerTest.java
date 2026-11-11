package org.edu_sharing.repository.server.tools.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

import static org.edu_sharing.repository.server.tools.security.SigningTestVector.*;
import static org.junit.jupiter.api.Assertions.*;

class RequestUserSignerTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final RequestUserSigner underTest = new RequestUserSigner(signer("SHA512withRSA"));

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
    void goldenVectorOfTheB2apiGivesTheSameBytes() throws Exception {
        String token = underTest.sign(AUDIENCE, API_KEY, "post", PATH, USER, IAT, JTI);
        assertEquals(TOKEN, token);
        assertTrue(signatureValid(token, "SHA512withRSA"));
    }

    @Test
    void headerAndClaimsAreExactlyWhatTheProtocolSays() throws Exception {
        String token = underTest.sign(AUDIENCE, API_KEY, "post", PATH, USER, IAT, JTI);
        JsonNode header = part(token, 0);
        assertEquals(java.util.List.of("alg", "typ", "kid"), java.util.stream.StreamSupport.stream(java.util.Spliterators.spliteratorUnknownSize(header.fieldNames(), 0), false).toList());
        assertEquals("RS512", header.get("alg").asText());
        assertEquals("bapi-user+jwt", header.get("typ").asText());
        assertEquals(KID, header.get("kid").asText());
        JsonNode claims = part(token, 1);
        assertEquals(APP_ID, claims.get("iss").asText());
        assertEquals(AUDIENCE, claims.get("aud").asText());
        assertEquals(USER, claims.get("sub").asText());
        assertEquals(IAT, claims.get("iat").asLong());
        assertEquals(IAT + 30, claims.get("exp").asLong());
        assertEquals(JTI, claims.get("jti").asText());
        assertEquals("POST", claims.get("htm").asText());
        assertEquals(PATH, claims.get("hpq").asText());
        assertEquals(ATH, claims.get("ath").asText());
        assertEquals(9, claims.size());
        assertFalse(token.contains(API_KEY), "the key itself is never part of the assertion");
    }

    @Test
    void theAlgorithmOfTheSignerIsUsedAndNamed() throws Exception {
        RequestUserSigner sha256 = new RequestUserSigner(signer("SHA256withRSA"));
        String token = sha256.sign(AUDIENCE, API_KEY, "GET", "/x", "alice");
        assertEquals("RS256", part(token, 0).get("alg").asText());
        assertTrue(signatureValid(token, "SHA256withRSA"));
        long iat = part(token, 1).get("iat").asLong();
        assertTrue(Math.abs(System.currentTimeMillis() / 1000 - iat) < 10, "signed now");
    }

    @Test
    void aChangedPartBreaksTheSignature() throws Exception {
        String token = underTest.sign(AUDIENCE, API_KEY, "POST", PATH, USER, IAT, JTI);
        String[] parts = token.split("\\.");
        String otherClaims = Base64.getUrlEncoder().withoutPadding().encodeToString(
                new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8).replace("repo-app-1", "mallory").getBytes(StandardCharsets.UTF_8));
        assertFalse(signatureValid(parts[0] + "." + otherClaims + "." + parts[2], "SHA512withRSA"));
    }

    @Test
    void jtiIsLongEnoughUrlSafeAndNotRepeated() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            String jti = RequestUserSigner.newJti();
            assertTrue(jti.matches("[A-Za-z0-9_-]{22}"), jti);
            assertTrue(seen.add(jti));
        }
    }

    @Test
    void aRepositoryThatCannotSignGivesNoAssertion() {
        RequestUserSigner noKey = new RequestUserSigner(signerOf(null, "SHA512withRSA"));
        assertThrows(HomeRepositorySigner.SigningFailedException.class, () -> noKey.sign(AUDIENCE, API_KEY, "GET", "/x", "alice"));
        RequestUserSigner odd = new RequestUserSigner(signer("SHA1withRSA"));
        assertThrows(HomeRepositorySigner.SigningFailedException.class, () -> odd.sign(AUDIENCE, API_KEY, "GET", "/x", "alice"), "an algorithm without a JWS name is not used");
    }
}
