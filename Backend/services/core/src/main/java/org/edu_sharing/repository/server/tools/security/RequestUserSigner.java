package org.edu_sharing.repository.server.tools.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Vouches for the user of an outgoing request: the repository tells an external service, with a JWS signed with its key ({@link HomeRepositorySigner}),
 * for which edu-sharing user the request is made. A service trusts a user only together with this assertion.
 * <p>
 * The assertion is a credential of its own. It goes into the header {@link #HEADER} (never into {@code Authorization}, which carries the API key) and
 * has nothing to do with the tokens the receiver hands on to its own services.
 * <ul>
 *   <li>JOSE header: {@code alg} ({@code RS256}/{@code RS384}/{@code RS512}, from the configured algorithm), {@code typ} = {@link #TYP},
 *       {@code kid} = RFC 7638 thumbprint of the public key. Nothing else (no {@code jku}, {@code jwk}, no app id).</li>
 *   <li>Claims: {@code iss} (app id of this repository), {@code aud} (name of the receiving service), {@code sub} (the user), {@code iat}, {@code exp}
 *       ({@link #LIFETIME_SECONDS} later), {@code jti} (16 random bytes, used once), {@code htm} (method), {@code hpq} (encoded path and query exactly as
 *       they go on the wire), {@code ath} (Base64url of the SHA-256 of the API key that is sent).</li>
 * </ul>
 * The assertion thus holds for this request only, for this key and this user. Both test suites check the same fixed token ({@code SigningTestVector}).
 */
@Component
public class RequestUserSigner {

    public static final String HEADER = "X-Edu-User-Assertion";
    public static final String TYP = "bapi-user+jwt";
    public static final long LIFETIME_SECONDS = 30;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final ObjectMapper JSON = new ObjectMapper();

    private final HomeRepositorySigner signer;

    public RequestUserSigner(HomeRepositorySigner signer) {
        this.signer = signer;
    }

    /**
     * Signs now, with a new {@code jti}, as this repository.
     *
     * @param audience        the name of the receiving service (what it expects in {@code aud})
     * @param rawPathAndQuery the encoded path and, if there is one, {@code ?} and the encoded query of the request URL, as sent
     * @param user            the authenticated user (not encoded)
     * @return the compact JWS for the header {@link #HEADER}
     * @throws HomeRepositorySigner.SigningFailedException when this repository cannot sign
     */
    public String sign(String audience, String apiKey, String method, String rawPathAndQuery, String user) {
        return sign(audience, apiKey, method, rawPathAndQuery, user, Instant.now().getEpochSecond(), newJti());
    }

    String sign(String audience, String apiKey, String method, String rawPathAndQuery, String user, long issuedAtSeconds, String jti) {
        String algorithm = signer.algorithm();
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", signer.jwsAlgorithm());
        header.put("typ", TYP);
        header.put("kid", signer.jwkThumbprint());
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("iss", signer.appId());
        claims.put("aud", audience);
        claims.put("sub", user);
        claims.put("iat", issuedAtSeconds);
        claims.put("exp", issuedAtSeconds + LIFETIME_SECONDS);
        claims.put("jti", jti);
        claims.put("htm", method.toUpperCase(Locale.ROOT));
        claims.put("hpq", rawPathAndQuery);
        claims.put("ath", apiKeyHash(apiKey));
        String signingInput = encode(header) + "." + encode(claims);
        byte[] signature = signer.signBytes(signingInput.getBytes(StandardCharsets.US_ASCII), algorithm);
        return signingInput + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
    }

    private static String encode(Map<String, Object> part) {
        try {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(JSON.writeValueAsString(part).getBytes(StandardCharsets.UTF_8));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    /** 16 random bytes, URL-safe Base64 without padding (22 characters). */
    public static String newJti() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Base64url of the SHA-256 of the API key: the key itself is never part of the assertion. */
    static String apiKeyHash(String apiKey) {
        try {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest(apiKey.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
