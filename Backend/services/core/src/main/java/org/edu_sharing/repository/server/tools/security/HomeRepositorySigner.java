package org.edu_sharing.repository.server.tools.security;

import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.spring.scope.refresh.annotations.RefreshScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
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
