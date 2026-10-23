package org.edu_sharing.service.altcha;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.alfresco.repo.cache.DefaultSimpleCache;
import org.altcha.altcha.v2.Altcha;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AltchaServiceTest {

    static final String SECRET = "test-secret";

    private AltchaService underTest;

    @BeforeEach
    void setUp() {
        underTest = new AltchaService(() -> config(SECRET), new DefaultSimpleCache<>());
    }

    static Config config(String secret) {
        return ConfigFactory.parseMap(Map.of(
                "enabled", true,
                "secret", secret,
                "cost", 100,
                "expiresSeconds", 600
        ));
    }

    /**
     * Solve the challenge and encode it the same way as the ALTCHA widget does
     */
    static String solve(Altcha.Challenge challenge) throws Exception {
        Altcha.Solution solution = Altcha.solveChallenge(challenge, Altcha.pbkdf2());
        assertNotNull(solution);
        JSONObject payload = new JSONObject()
                .put("challenge", new JSONObject(challenge.toJson()))
                .put("solution", new JSONObject()
                        .put("counter", solution.counter())
                        .put("derivedKey", solution.derivedKey()));
        return Base64.getEncoder().encodeToString(payload.toString().getBytes(StandardCharsets.UTF_8));
    }

    static String validPayload(AltchaService service) throws Exception {
        return solve(service.createChallenge(AltchaService.ACTION_REPORT, "127.0.0.1"));
    }

    @Test
    void validSolutionPasses() throws Exception {
        String payload = validPayload(underTest);
        assertDoesNotThrow(() -> underTest.verify(payload, AltchaService.ACTION_REPORT));
    }

    @Test
    void challengeUsesConfiguredAlgorithmAndCost() throws Exception {
        Altcha.Challenge challenge = underTest.createChallenge(AltchaService.ACTION_REPORT, "127.0.0.1");
        assertEquals(AltchaService.ALGORITHM, challenge.parameters().algorithm());
        assertEquals(100, challenge.parameters().cost());
        assertEquals(AltchaService.ACTION_REPORT, challenge.parameters().data().get("action"));
    }

    @Test
    void tamperedSignatureFails() throws Exception {
        Altcha.Challenge challenge = underTest.createChallenge(AltchaService.ACTION_REPORT, "127.0.0.1");
        String signature = challenge.signature();
        String tampered = (signature.charAt(0) == 'a' ? "b" : "a") + signature.substring(1);
        String payload = solve(new Altcha.Challenge(challenge.parameters(), tampered));

        AltchaVerificationException e = assertThrows(AltchaVerificationException.class,
                () -> underTest.verify(payload, AltchaService.ACTION_REPORT));
        assertEquals(403, e.getReason().getStatus());
    }

    @Test
    void solutionOfOtherInstanceFails() throws Exception {
        // an instance with a random secret must not accept solutions of other instances
        AltchaService otherInstance = new AltchaService(() -> config(""), new DefaultSimpleCache<>());
        String payload = validPayload(otherInstance);

        AltchaVerificationException e = assertThrows(AltchaVerificationException.class,
                () -> underTest.verify(payload, AltchaService.ACTION_REPORT));
        assertEquals(403, e.getReason().getStatus());
    }

    @Test
    void expiredFails() throws Exception {
        Altcha.Challenge challenge = Altcha.createChallenge(new Altcha.CreateChallengeOptions()
                .algorithm(AltchaService.ALGORITHM)
                .cost(100)
                .expiresAt(System.currentTimeMillis() / 1000 - 60)
                .hmacSignatureSecret(SECRET)
                .data(Map.of("action", AltchaService.ACTION_REPORT)));
        String payload = solve(challenge);

        AltchaVerificationException e = assertThrows(AltchaVerificationException.class,
                () -> underTest.verify(payload, AltchaService.ACTION_REPORT));
        assertEquals(403, e.getReason().getStatus());
    }

    @Test
    void otherActionFails() throws Exception {
        String payload = solve(underTest.createChallenge("other", "127.0.0.1"));

        AltchaVerificationException e = assertThrows(AltchaVerificationException.class,
                () -> underTest.verify(payload, AltchaService.ACTION_REPORT));
        assertEquals(403, e.getReason().getStatus());
    }

    @Test
    void missingOrGarbagePayloadFails() {
        for (String payload : new String[]{null, "", "garbage", "x".repeat(5000)}) {
            AltchaVerificationException e = assertThrows(AltchaVerificationException.class,
                    () -> underTest.verify(payload, AltchaService.ACTION_REPORT));
            assertEquals(403, e.getReason().getStatus());
        }
    }

    @Test
    void replayReturns409() throws Exception {
        String payload = validPayload(underTest);
        underTest.verify(payload, AltchaService.ACTION_REPORT);

        AltchaVerificationException e = assertThrows(AltchaVerificationException.class,
                () -> underTest.verify(payload, AltchaService.ACTION_REPORT));
        assertEquals(409, e.getReason().getStatus());
    }

    @Test
    void clientKeyUsesIpv6Prefix() {
        assertEquals("203.0.113.7", AltchaService.clientKey("203.0.113.7"));
        assertEquals(AltchaService.clientKey("2001:db8:1:2:aaaa::1"), AltchaService.clientKey("2001:db8:1:2:bbbb::2"));
        assertNotEquals(AltchaService.clientKey("2001:db8:1:2::1"), AltchaService.clientKey("2001:db8:1:3::1"));
    }
}
