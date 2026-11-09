package org.edu_sharing.repository.server.tools.security;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;

/**
 * Vouches for the user of an outgoing request: the repository tells an external service, signed with its key ({@link HomeRepositorySigner}),
 * for which edu-sharing user the request is made. A service trusts a user name in a header only together with this signature.
 * <p>
 * What is signed (lines joined by {@code \n}, no trailing newline): the {@code scheme} (names the format, e.g. {@code bapi-user-v1}), the app id of
 * this repository, the SHA-256 of the API key that is sent, the method, the encoded path and query exactly as they go on the wire, the time stamp
 * (ms), a nonce that is used once and the user (URL-encoded, as in the header). The signature thus holds for this request only, for this key and this
 * user. The receiver builds the same string from the request it gets and never takes it from a header. The headers are the constants
 * {@code HEADER_*}; a service that wants another format uses another {@code scheme}.
 */
@Component
public class RequestUserSigner {

    public static final String HEADER_USER = "X-Edu-User-Id";
    public static final String HEADER_APP_ID = "X-Edu-App-Id";
    public static final String HEADER_TIMESTAMP = "X-Edu-App-Ts";
    public static final String HEADER_NONCE = "X-Edu-App-Nonce";
    public static final String HEADER_SIGNATURE = "X-Edu-App-Sig";
    public static final String HEADER_ALGORITHM = "X-Edu-App-SignedAlg";

    private static final SecureRandom RANDOM = new SecureRandom();

    private final HomeRepositorySigner signer;

    public RequestUserSigner(HomeRepositorySigner signer) {
        this.signer = signer;
    }

    /** The values of the headers a signed request carries. {@code user} is URL-encoded (UTF-8), exactly as it goes into the header. */
    public record SignedHeaders(String user, String appId, String timestamp, String nonce, String signature, String algorithm) {
    }

    /**
     * Signs now, with a new nonce, as this repository.
     *
     * @param rawPathAndQuery the encoded path and, if there is one, {@code ?} and the encoded query of the request URL, as sent
     * @param user            the authenticated user (not encoded)
     * @throws HomeRepositorySigner.SigningFailedException when this repository cannot sign
     */
    public SignedHeaders sign(String scheme, String apiKey, String method, String rawPathAndQuery, String user) {
        return sign(scheme, apiKey, method, rawPathAndQuery, user, System.currentTimeMillis(), newNonce());
    }

    SignedHeaders sign(String scheme, String apiKey, String method, String rawPathAndQuery, String user, long timestampMillis, String nonce) {
        String appId = signer.appId();
        String algorithm = signer.algorithm();
        String userEncoded = encodeUser(user);
        String data = signedString(scheme, appId, apiKey, method, rawPathAndQuery, timestampMillis, nonce, userEncoded);
        return new SignedHeaders(userEncoded, appId, Long.toString(timestampMillis), nonce, signer.sign(data, algorithm), algorithm);
    }

    /** Lines joined by {@code \n}, no trailing newline. */
    public static String signedString(String scheme, String appId, String apiKey, String method, String rawPathAndQuery, long timestampMillis,
                                      String nonce, String userEncoded) {
        return String.join("\n", scheme, appId, sha256Hex(apiKey), method.toUpperCase(Locale.ROOT), rawPathAndQuery,
                Long.toString(timestampMillis), nonce, userEncoded);
    }

    /** The name as it goes into the header: UTF-8, percent-encoded, so that no character beyond ASCII reaches the header. */
    public static String encodeUser(String user) {
        return java.net.URLEncoder.encode(user, StandardCharsets.UTF_8);
    }

    /** 16 random bytes, URL-safe Base64 without padding (22 characters). */
    public static String newNonce() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String sha256Hex(String value) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
