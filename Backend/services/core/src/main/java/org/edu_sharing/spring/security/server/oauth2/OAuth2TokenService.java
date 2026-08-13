package org.edu_sharing.spring.security.server.oauth2;

import com.nimbusds.jwt.JWTParser;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.spring.security.server.oauth2.config.OAuth2ConfigService;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OAuth2TokenService {

    private final OAuth2ConfigService oAuth2ConfigService;
    private final TrustedIssuerRegistry trustedIssuerRegistry;

    private volatile JwtDecoder ownDecoder;
    private volatile boolean ownDecoderResolved;

    public OAuth2TokenService(OAuth2ConfigService oAuth2ConfigService, TrustedIssuerRegistry trustedIssuerRegistry) {
        log.info("Initializing OAuth2TokenService");
        this.oAuth2ConfigService = oAuth2ConfigService;
        this.trustedIssuerRegistry = trustedIssuerRegistry;
    }

    /**
     * Validates a bearer token and returns the edu-sharing user it authenticates.
     * <p>
     * The token is routed to a decoder by its iss claim: issuers listed in
     * {@code security.authentication.oauth2.trustedIssuers} are decoded with their own key set, everything
     * else falls back to the key set of the authorization server of this repository. Note that the issuer
     * of our own tokens is derived from the request url (no fixed issuer is configured in
     * {@link OAuth2AuthorizationServerConfig}), which is why our own tokens are the fallback rather than
     * another entry to match against.
     *
     * @return the user name, or null if the token could not be verified
     */
    public String extractUsername(String token) {
        String issuer = readIssuerUnverified(token);
        if (issuer == null) {
            return null;
        }

        TrustedIssuerRegistry.TrustedIssuerContext context = trustedIssuerRegistry.get(issuer);
        if (context != null) {
            // the claims of the parse above are unverified and must only be used for routing - from
            // here on everything is read off the validated token
            Jwt jwt = decode(context.getDecoder(), token, issuer, "trusted issuer " + issuer);
            if (jwt == null) {
                return null;
            }
            String username = context.resolveUsername(jwt);
            if (username == null) {
                log.warn("token of trusted issuer {} is valid but carries no user name claim", issuer);
            }
            return username;
        }

        JwtDecoder own = getOwnDecoder();
        if (own == null) {
            log.warn("got a bearer token of issuer {} but neither is that issuer trusted nor is the "
                    + "authorization server of this repository enabled", issuer);
            return null;
        }
        // the issuer of our own tokens is derived from the request url, so it cannot be matched against
        // a fixed string - an unknown issuer is therefore treated as one of ours. this is logged because
        // otherwise a trustedIssuers entry that does not match makes a foreign token fail against our own
        // key set, which looks like a broken signature rather than a configuration problem.
        if (!trustedIssuerRegistry.isEmpty()) {
            log.info("issuer {} is not configured in security.authentication.oauth2.trustedIssuers, "
                    + "falling back to the key set of the local authorization server", issuer);
        }
        Jwt jwt = decode(own, token, issuer, "the local authorization server");
        return jwt == null ? null : jwt.getSubject();
    }

    /**
     * reads the iss claim without verifying the token, used to pick the decoder
     */
    private String readIssuerUnverified(String token) {
        try {
            return JWTParser.parse(token).getJWTClaimsSet().getIssuer();
        } catch (ParseException e) {
            log.warn("bearer token is not a readable jwt: {}", e.getMessage());
            return null;
        }
    }

    /**
     * @param keySet what the token was verified against, named in the log so that a token failing
     *               against the wrong key set is not mistaken for a broken signature
     */
    private Jwt decode(JwtDecoder decoder, String token, String issuer, String keySet) {
        try {
            return decoder.decode(token);
        } catch (JwtValidationException e) {
            // claim level rejections: expired, wrong issuer, wrong audience, wrong authorized party
            log.warn("rejected bearer token of issuer {}: {}", issuer,
                    e.getErrors().stream().map(OAuth2Error::getDescription).collect(Collectors.joining("; ")));
        } catch (JwtException e) {
            // signature, malformed token or the key set could not be retrieved
            log.warn("could not verify bearer token of issuer {} against the key set of {}: {}",
                    issuer, keySet, e.getMessage());
        }
        return null;
    }

    /**
     * The decoder for tokens issued by this repository, or null when the authorization server is disabled.
     * Built once instead of per call - each build sets up its own key set cache, so building it per request
     * means one http request to our own jwks endpoint for every api call.
     */
    private JwtDecoder getOwnDecoder() {
        if (!ownDecoderResolved) {
            synchronized (this) {
                if (!ownDecoderResolved) {
                    if (oAuth2ConfigService.getDefaultConfig().isEnabled()) {
                        ownDecoder = NimbusJwtDecoder
                                .withJwkSetUri(ApplicationInfoList.getHomeRepository().getBaseUrl()
                                        + "/edu-sharing/oauth2server/jwks")
                                .build();
                    } else {
                        log.info("authorization server is disabled, only tokens of trusted issuers are accepted");
                    }
                    ownDecoderResolved = true;
                }
            }
        }
        return ownDecoder;
    }

    public String getAccessToken(HttpServletRequest request) {

        String accessToken = request.getParameter(CCConstants.REQUEST_PARAM_ACCESSTOKEN);
        if(accessToken != null && !accessToken.trim().isEmpty()) return accessToken;

        String authHdr = request.getHeader("Authorization");
        if (authHdr != null) {
            if (authHdr.length() > 6 && authHdr.substring(0, 6).equalsIgnoreCase("Bearer")) {
                return authHdr.substring(6).trim();
            }
        }
        return null;
    }
}
