package org.edu_sharing.repository.server.tools.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Vouches for this repository itself (no user) when it calls an external service, e.g. for the version of the rendering service 2 in the status
 * report: a short-lived JWS signed with its key ({@link HomeRepositorySigner}), which the service verifies with the public key of its registration.
 * <p>
 * It goes into the header {@link #HEADER} (never into {@code Authorization}, which services read as the token of a user).
 * <ul>
 *   <li>JOSE header: {@code alg} ({@code RS256}/{@code RS384}/{@code RS512}), {@code typ} = {@link #TYP}, {@code kid} = RFC 7638 thumbprint of the
 *       public key (see {@link HomeRepositorySigner#signJws}).</li>
 *   <li>Claims: {@code iss} (app id of this repository), {@code aud} (name of the receiving service), {@code iat}, {@code exp}
 *       ({@link #LIFETIME_SECONDS} later), {@code jti} (16 random bytes).</li>
 * </ul>
 * It is not bound to a request (no method or path: a proxy in front of the service may rewrite them), so it is for read-only calls only.
 */
@Component
public class AppAssertionSigner {

    public static final String HEADER = "X-Edu-App-Assertion";
    public static final String TYP = "edu-app+jwt";
    public static final long LIFETIME_SECONDS = 30;

    private final HomeRepositorySigner signer;

    public AppAssertionSigner(HomeRepositorySigner signer) {
        this.signer = signer;
    }

    /**
     * Signs now, with a new {@code jti}, as this repository.
     *
     * @param audience the name of the receiving service (what it expects in {@code aud})
     * @return the compact JWS for the header {@link #HEADER}
     * @throws HomeRepositorySigner.SigningFailedException when this repository cannot sign
     */
    public String sign(String audience) {
        return sign(audience, Instant.now().getEpochSecond(), RequestUserSigner.newJti());
    }

    String sign(String audience, long issuedAtSeconds, String jti) {
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("iss", signer.appId());
        claims.put("aud", audience);
        claims.put("iat", issuedAtSeconds);
        claims.put("exp", issuedAtSeconds + LIFETIME_SECONDS);
        claims.put("jti", jti);
        return signer.signJws(TYP, claims);
    }
}
