package org.edu_sharing.spring.security.server.oauth2;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.edu_sharing.spring.security.server.oauth2.config.OAuth2Config;
import org.edu_sharing.spring.security.server.oauth2.config.OAuth2ConfigService;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds the decoders for external identity providers whose access tokens are accepted as bearer
 * tokens, configured via {@code security.authentication.oauth2.trustedIssuers}.
 * <p>
 * Tokens are routed to a decoder by their iss claim. An issuer that is not configured here has no
 * decoder and its tokens are therefore never accepted.
 */
@Slf4j
@Component
public class TrustedIssuerRegistry {

    private final Map<String, TrustedIssuerContext> contexts;

    public TrustedIssuerRegistry(OAuth2ConfigService oAuth2ConfigService) {
        this.contexts = Collections.unmodifiableMap(build(oAuth2ConfigService));
    }

    private Map<String, TrustedIssuerContext> build(OAuth2ConfigService oAuth2ConfigService) {
        List<OAuth2Config.TrustedIssuer> configured = oAuth2ConfigService.getDefaultConfig().getTrustedIssuers();
        if (configured == null || configured.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, TrustedIssuerContext> result = new HashMap<>();
        for (OAuth2Config.TrustedIssuer issuer : configured) {
            if (StringUtils.isBlank(issuer.getIssuerUri())) {
                throw new IllegalStateException(
                        "security.authentication.oauth2.trustedIssuers: issuerUri must not be empty");
            }
            // without one of these every token of that issuer would be accepted, regardless of which
            // client requested it or which service it was meant for. failing at startup is the only
            // way to make that visible - a missing check is silent at runtime.
            if (StringUtils.isBlank(issuer.getAudience()) && StringUtils.isBlank(issuer.getAuthorizedParty())) {
                throw new IllegalStateException(
                        "security.authentication.oauth2.trustedIssuers: at least one of audience / authorizedParty "
                                + "must be set for issuer " + issuer.getIssuerUri());
            }
            result.put(issuer.getIssuerUri(), new TrustedIssuerContext(issuer));
            log.info("accepting access tokens of trusted issuer {} (audience: '{}', authorizedParty: '{}')",
                    issuer.getIssuerUri(), issuer.getAudience(), issuer.getAuthorizedParty());
        }
        return result;
    }

    /**
     * @return the context for the given iss claim, or null if that issuer is not trusted
     */
    public TrustedIssuerContext get(String issuer) {
        return issuer == null ? null : contexts.get(issuer);
    }

    public boolean isEmpty() {
        return contexts.isEmpty();
    }

    public static class TrustedIssuerContext {

        private final OAuth2Config.TrustedIssuer config;
        private volatile JwtDecoder decoder;

        TrustedIssuerContext(OAuth2Config.TrustedIssuer config) {
            this.config = config;
        }

        /**
         * The decoder is built on first use rather than at startup: building it fetches the key set
         * (and the discovery document) from the identity provider, so an idp that is temporarily
         * unreachable would otherwise prevent the repository from starting.
         */
        public JwtDecoder getDecoder() {
            JwtDecoder result = decoder;
            if (result == null) {
                synchronized (this) {
                    result = decoder;
                    if (result == null) {
                        result = decoder = buildDecoder();
                    }
                }
            }
            return result;
        }

        /**
         * resolves the edu-sharing user name from an already validated token
         */
        public String resolveUsername(Jwt jwt) {
            if (StringUtils.isNotBlank(config.getUsername())) {
                return config.getUsername();
            }
            return jwt.getClaimAsString(StringUtils.defaultIfBlank(config.getUsernameClaim(), JwtClaimNames.SUB));
        }

        private JwtDecoder buildDecoder() {
            NimbusJwtDecoder result = StringUtils.isNotBlank(config.getJwkSetUri())
                    ? NimbusJwtDecoder.withJwkSetUri(config.getJwkSetUri()).build()
                    : NimbusJwtDecoder.withIssuerLocation(config.getIssuerUri()).build();

            List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
            validators.add(new JwtTimestampValidator());
            validators.add(new JwtIssuerValidator(config.getIssuerUri()));
            if (StringUtils.isNotBlank(config.getAudience())) {
                validators.add(audienceValidator(config.getAudience()));
            }
            if (StringUtils.isNotBlank(config.getAuthorizedParty())) {
                validators.add(new JwtClaimValidator<String>("azp", config.getAuthorizedParty()::equals));
            }
            result.setJwtValidator(new DelegatingOAuth2TokenValidator<>(validators));
            return result;
        }

        private static OAuth2TokenValidator<Jwt> audienceValidator(String audience) {
            return jwt -> jwt.getAudience() != null && jwt.getAudience().contains(audience)
                    ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(new OAuth2Error(
                    OAuth2ErrorCodes.INVALID_TOKEN,
                    "the required audience '" + audience + "' is missing in the aud claim",
                    null));
        }
    }
}
