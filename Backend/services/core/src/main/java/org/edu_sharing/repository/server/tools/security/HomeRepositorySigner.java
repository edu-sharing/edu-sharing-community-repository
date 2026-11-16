package org.edu_sharing.repository.server.tools.security;

import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.spring.scope.refresh.annotations.RefreshScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.Signature;
import java.util.Base64;

/**
 * This repository signs with its own private key (the key of its home application, the one that is also published in {@code /metadata}).
 * <p>
 * For everything that has to prove to an external service that it comes from this repository: the proxy to the B-API ({@code RequestUserSigner}),
 * and whatever follows. The protocol (what is signed, which headers) belongs to the user of this class, this class only knows key and algorithm.
 * <p>
 * The algorithm is {@code security.sso.authByApp.alg.defaultSign} (the setting that is also used for the calls of the repository to remote
 * repositories), read through {@link Value} in a {@link RefreshScope} bean: a change of the configuration is used from the next call on, without a
 * restart. The scoped proxy is made with CGLIB: this class stays non-final, its public methods non-final.
 */
@Component
@RefreshScope
public class HomeRepositorySigner {

    public static final String DEFAULT_ALGORITHM = "SHA512withRSA";

    /** Anything that goes wrong on the way to a signature (no key, a key that cannot be read, an algorithm that is not there). */
    public static class SigningFailedException extends RuntimeException {
        public SigningFailedException(String message, Throwable cause) {
            super(message, cause);
        }

        public SigningFailedException(String message) {
            super(message);
        }
    }

    private final String algorithm;

    private volatile String parsedKeySource;
    private volatile PrivateKey parsedKey;

    public HomeRepositorySigner(@Value("${security.sso.authByApp.alg.defaultSign:" + DEFAULT_ALGORITHM + "}") String algorithm) {
        this.algorithm = algorithm == null || algorithm.isBlank() ? DEFAULT_ALGORITHM : algorithm.trim();
    }

    /** The algorithm {@link #sign(String)} uses (the configured one). */
    public String algorithm() {
        return algorithm;
    }

    /** The app id of this repository. */
    public String appId() {
        ApplicationInfo home = home();
        if (home.getAppId() == null || home.getAppId().isBlank()) {
            throw new SigningFailedException("this repository has no app id");
        }
        return home.getAppId();
    }

    /** Base64 of the signature of {@code data} (UTF-8) with the configured algorithm. */
    public String sign(String data) {
        return sign(data, algorithm);
    }

    /** Base64 of the signature of {@code data} (UTF-8) with the given algorithm, e.g. when the receiver names the one it accepts. */
    public String sign(String data, String algorithm) {
        try {
            Signature signature = Signature.getInstance(algorithm);
            signature.initSign(privateKey());
            signature.update(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (GeneralSecurityException e) {
            throw new SigningFailedException("could not sign with " + algorithm, e);
        }
    }

    /** The raw signature of {@code data} with the given algorithm (what a JWS carries, Base64url-encoded by the caller). */
    public byte[] signBytes(byte[] data, String algorithm) {
        try {
            Signature signature = Signature.getInstance(algorithm);
            signature.initSign(privateKey());
            signature.update(data);
            return signature.sign();
        } catch (GeneralSecurityException e) {
            throw new SigningFailedException("could not sign with " + algorithm, e);
        }
    }

    /**
     * The JWS name ({@code RS256}, {@code RS384}, {@code RS512}) of the configured algorithm. Any other algorithm cannot be named in a JWS here.
     */
    public String jwsAlgorithm() {
        switch (algorithm) {
            case "SHA256withRSA":
                return "RS256";
            case "SHA384withRSA":
                return "RS384";
            case "SHA512withRSA":
                return "RS512";
            default:
                throw new SigningFailedException("the algorithm " + algorithm + " has no JWS name");
        }
    }

    /**
     * The RFC 7638 thumbprint (SHA-256, Base64url) of the public key that belongs to the private key of this repository: what a JWS names in {@code kid},
     * so that the receiver finds the key it has pinned. Derived from the private key, so it is always the key that really signs.
     */
    public String jwkThumbprint() {
        try {
            if (!(privateKey() instanceof RSAPrivateCrtKey)) {
                throw new SigningFailedException("the key of this repository is no RSA key with its public parts");
            }
            RSAPrivateCrtKey key = (RSAPrivateCrtKey) privateKey();
            // the members in lexicographic order, no blanks, as RFC 7638 says
            String json = "{\"e\":\"" + unsigned(key.getPublicExponent()) + "\",\"kty\":\"RSA\",\"n\":\"" + unsigned(key.getModulus()) + "\"}";
            return Base64.getUrlEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest(json.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new SigningFailedException("could not read the key of this repository", e);
        }
    }

    private static String unsigned(java.math.BigInteger value) {
        byte[] bytes = value.toByteArray();
        int start = bytes.length > 1 && bytes[0] == 0 ? 1 : 0;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(java.util.Arrays.copyOfRange(bytes, start, bytes.length));
    }

    /** The home application with its keys; a method of its own so that a test can replace it. */
    protected ApplicationInfo homeRepository() {
        return ApplicationInfoList.getHomeRepository();
    }

    private ApplicationInfo home() {
        ApplicationInfo home = homeRepository();
        if (home == null) {
            throw new SigningFailedException("this repository has no home application");
        }
        return home;
    }

    private PrivateKey privateKey() throws GeneralSecurityException {
        String pem = home().getPrivateKey();
        if (pem == null || pem.isBlank()) {
            throw new SigningFailedException("this repository has no private key");
        }
        if (pem.equals(parsedKeySource)) {
            return parsedKey;
        }
        PrivateKey key = new Signing().getPemPrivateKey(pem, CCConstants.SECURITY_KEY_ALGORITHM);
        parsedKey = key;
        parsedKeySource = pem;
        return key;
    }
}
