package org.edu_sharing.repository.server.tools.security;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.edu_sharing.repository.server.tools.security.SigningTestVector.*;
import static org.junit.jupiter.api.Assertions.*;

class RequestUserSignerTest {

    private final RequestUserSigner underTest = new RequestUserSigner(signer("SHA512withRSA"));

    @Test
    void goldenVectorOfTheB2apiGivesTheSameStringAndTheSameSignature() throws Exception {
        RequestUserSigner.SignedHeaders signed = underTest.sign(SCHEME, API_KEY, "post", PATH, USER, TS, NONCE);

        String data = RequestUserSigner.signedString(SCHEME, APP_ID, API_KEY, "post", PATH, TS, NONCE, signed.user());
        assertEquals(EXPECTED_SIGNED_STRING, data.replace("\n", "\\n"));
        assertEquals(EXPECTED_SIGNATURE, signed.signature());
        assertEquals(USER_ENCODED, signed.user());
        assertEquals(APP_ID, signed.appId());
        assertEquals(Long.toString(TS), signed.timestamp());
        assertEquals(NONCE, signed.nonce());
        assertEquals("SHA512withRSA", signed.algorithm());
        assertTrue(verify(data, signed.signature(), "SHA512withRSA"));
    }

    @Test
    void theSignedStringHasEightLinesInAFixedOrderAndHoldsTheHashOfTheKeyNeverTheKey() {
        String data = RequestUserSigner.signedString("scheme-x", "app", "key", "get", "/p?q=1", 5L, "nonce", "alice");
        String[] lines = data.split("\n", -1);
        assertEquals(8, lines.length);
        assertEquals("scheme-x", lines[0], "another service names its own format");
        assertEquals("app", lines[1]);
        assertEquals(64, lines[2].length());
        assertEquals(RequestUserSigner.sha256Hex("key"), lines[2]);
        assertFalse(data.contains("key\n"));
        assertEquals("GET", lines[3]);
        assertEquals("/p?q=1", lines[4]);
        assertEquals("5", lines[5]);
        assertEquals("nonce", lines[6]);
        assertEquals("alice", lines[7]);
    }

    @Test
    void anyChangedPartBreaksTheSignature() throws Exception {
        RequestUserSigner.SignedHeaders signed = underTest.sign(SCHEME, API_KEY, "POST", PATH, USER, TS, NONCE);
        String user = signed.user();
        assertTrue(verify(RequestUserSigner.signedString(SCHEME, APP_ID, API_KEY, "POST", PATH, TS, NONCE, user), signed.signature(), "SHA512withRSA"));
        for (String changed : new String[]{
                RequestUserSigner.signedString("other-v1", APP_ID, API_KEY, "POST", PATH, TS, NONCE, user),
                RequestUserSigner.signedString(SCHEME, "other", API_KEY, "POST", PATH, TS, NONCE, user),
                RequestUserSigner.signedString(SCHEME, APP_ID, "other-key", "POST", PATH, TS, NONCE, user),
                RequestUserSigner.signedString(SCHEME, APP_ID, API_KEY, "GET", PATH, TS, NONCE, user),
                RequestUserSigner.signedString(SCHEME, APP_ID, API_KEY, "POST", PATH + "x", TS, NONCE, user),
                RequestUserSigner.signedString(SCHEME, APP_ID, API_KEY, "POST", PATH, TS + 1, NONCE, user),
                RequestUserSigner.signedString(SCHEME, APP_ID, API_KEY, "POST", PATH, TS, "BBBBBBBBBBBBBBBBBBBBBB", user),
                RequestUserSigner.signedString(SCHEME, APP_ID, API_KEY, "POST", PATH, TS, NONCE, "mallory"),
        }) {
            assertFalse(verify(changed, signed.signature(), "SHA512withRSA"));
        }
    }

    @Test
    void theAlgorithmOfTheSignerIsUsedAndNamed() throws Exception {
        RequestUserSigner sha256 = new RequestUserSigner(signer("SHA256withRSA"));
        RequestUserSigner.SignedHeaders signed = sha256.sign(SCHEME, API_KEY, "GET", "/x", "alice");
        assertEquals("SHA256withRSA", signed.algorithm());
        assertTrue(verify(RequestUserSigner.signedString(SCHEME, APP_ID, API_KEY, "GET", "/x", Long.parseLong(signed.timestamp()), signed.nonce(), "alice"),
                signed.signature(), "SHA256withRSA"));
        assertTrue(Math.abs(System.currentTimeMillis() - Long.parseLong(signed.timestamp())) < 10_000, "signed now");
    }

    @Test
    void theUserGoesIntoTheHeaderAsAsciiAndComesBackUnchanged() {
        for (String user : new String[]{"alice", "Jürgen", "a+b", "x@y.z", "ünï_cödé-1", "GUEST"}) {
            String encoded = RequestUserSigner.encodeUser(user);
            assertTrue(encoded.chars().allMatch(c -> c < 128), encoded);
            assertEquals(user, java.net.URLDecoder.decode(encoded, java.nio.charset.StandardCharsets.UTF_8));
        }
        assertEquals("a%2Bb", RequestUserSigner.encodeUser("a+b"));
    }

    @Test
    void nonceIsLongEnoughUrlSafeAndNotRepeated() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            String nonce = RequestUserSigner.newNonce();
            assertTrue(nonce.matches("[A-Za-z0-9_-]{22}"), nonce);
            assertTrue(seen.add(nonce));
        }
    }

    @Test
    void aRepositoryThatCannotSignGivesNoHeaders() {
        RequestUserSigner noKey = new RequestUserSigner(signerOf(null, "SHA512withRSA"));
        assertThrows(HomeRepositorySigner.SigningFailedException.class, () -> noKey.sign(SCHEME, API_KEY, "GET", "/x", "alice"));
    }
}
