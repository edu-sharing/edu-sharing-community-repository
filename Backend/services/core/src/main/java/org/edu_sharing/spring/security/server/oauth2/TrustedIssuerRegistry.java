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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Holds the decoders for external identity providers whose access tokens are accepted as bearer
 * tokens, configured via {@code security.authentication.oauth2.trustedIssuers}.
 * <p>
 * Tokens are routed to a decoder by their iss claim. An issuer that is not configured here has no
 * decoder and its tokens are therefore never accepted.
 * <p>
 * An issuer may be configured more than once: one entry per client that is allowed to call us, each
 * with its own audience / authorizedParty and its own user mapping. All entries of one issuer share
 * a decoder - the iss claim identifies the key set - and a token is matched against them once it has
 * been decoded, see {@link TrustedIssuerGroup#match(Jwt)}.
 */
@Slf4j
@Component
public class TrustedIssuerRegistry {

    static final String CLAIM_AZP = "azp";

    private static final String CONFIG_PATH = "security.authentication.oauth2.trustedIssuers";

    private final Map<String, TrustedIssuerGroup> groups;

    public TrustedIssuerRegistry(OAuth2ConfigService oAuth2ConfigService) {
        this.groups = Collections.unmodifiableMap(build(oAuth2ConfigService));
    }

    private Map<String, TrustedIssuerGroup> build(OAuth2ConfigService oAuth2ConfigService) {
        List<OAuth2Config.TrustedIssuer> configured = oAuth2ConfigService.getDefaultConfig().getTrustedIssuers();
        if (configured == null || configured.isEmpty()) {
            return new HashMap<>();
        }

        // grouped in configuration order so that the entries of one issuer are matched top down
        Map<String, List<OAuth2Config.TrustedIssuer>> byIssuer = new LinkedHashMap<>();
        for (OAuth2Config.TrustedIssuer issuer : configured) {
            if (StringUtils.isBlank(issuer.getIssuerUri())) {
                throw new IllegalStateException(CONFIG_PATH + ": issuerUri must not be empty");
            }
            // without one of these every token of that issuer would be accepted, regardless of which
            // client requested it or which service it was meant for. failing at startup is the only
            // way to make that visible - a missing check is silent at runtime.
            if (StringUtils.isBlank(issuer.getAudience()) && StringUtils.isBlank(issuer.getAuthorizedParty())) {
                throw new IllegalStateException(CONFIG_PATH + ": at least one of audience / authorizedParty "
                        + "must be set for issuer " + issuer.getIssuerUri());
            }
            byIssuer.computeIfAbsent(issuer.getIssuerUri(), k -> new ArrayList<>()).add(issuer);
        }

        Map<String, TrustedIssuerGroup> result = new HashMap<>();
        byIssuer.forEach((issuerUri, entries) -> {
            verifyDistinguishable(issuerUri, entries);
            result.put(issuerUri, new TrustedIssuerGroup(issuerUri, jwkSetUriOf(issuerUri, entries), entries));
            entries.forEach(entry -> log.info(
                    "accepting access tokens of trusted issuer {} (audience: '{}', authorizedParty: '{}')",
                    issuerUri, entry.getAudience(), entry.getAuthorizedParty()));
        });
        return result;
    }

    /**
     * Entries of one issuer are told apart by audience / authorizedParty only, so two entries sharing
     * that pair are indistinguishable - the second one could never be reached and its user mapping
     * would silently do nothing.
     */
    private void verifyDistinguishable(String issuerUri, List<OAuth2Config.TrustedIssuer> entries) {
        Set<String> seen = new HashSet<>();
        for (OAuth2Config.TrustedIssuer entry : entries) {
            String key = StringUtils.trimToEmpty(entry.getAudience()) + " / "
                    + StringUtils.trimToEmpty(entry.getAuthorizedParty());
            if (!seen.add(key)) {
                throw new IllegalStateException(CONFIG_PATH + ": issuer " + issuerUri + " is configured twice with "
                        + "the same audience / authorizedParty (audience: '" + entry.getAudience()
                        + "', authorizedParty: '" + entry.getAuthorizedParty() + "') - entries of one issuer are "
                        + "told apart by these two values, so the second one could never be reached");
            }
        }
    }

    /**
     * The iss claim identifies the keys a token is signed with, so all entries of one issuer must read
     * their key set from the same place. Differing values mean one of them is a typo, which would show
     * up as a broken signature rather than as a configuration problem.
     */
    private String jwkSetUriOf(String issuerUri, List<OAuth2Config.TrustedIssuer> entries) {
        Set<String> jwkSetUris = entries.stream()
                .map(entry -> StringUtils.trimToEmpty(entry.getJwkSetUri()))
                .collect(Collectors.toSet());
        if (jwkSetUris.size() > 1) {
            throw new IllegalStateException(CONFIG_PATH + ": issuer " + issuerUri + " is configured with differing "
                    + "jwkSetUri values " + jwkSetUris + " - all entries of one issuer must use the same key set");
        }
        return jwkSetUris.iterator().next();
    }

    /**
     * @return the entries configured for the given iss claim, or null if that issuer is not trusted
     */
    public TrustedIssuerGroup get(String issuer) {
        return issuer == null ? null : groups.get(issuer);
    }

    public boolean isEmpty() {
        return groups.isEmpty();
    }

    /**
     * All entries configured for one issuer uri. They share the key set and the issuer / expiry checks
     * and differ only in what binds a token to a client and in the user it is mapped to.
     */
    public static class TrustedIssuerGroup {

        private final String issuerUri;
        private final String jwkSetUri;
        private final List<TrustedIssuerContext> contexts;
        private volatile JwtDecoder decoder;

        TrustedIssuerGroup(String issuerUri, String jwkSetUri, List<OAuth2Config.TrustedIssuer> entries) {
            this.issuerUri = issuerUri;
            this.jwkSetUri = jwkSetUri;
            this.contexts = entries.stream().map(TrustedIssuerContext::new).collect(Collectors.toList());
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
         * Picks the entry a decoded token belongs to, first match in configuration order wins.
         *
         * @return the matching entry, or null if the token fits none of them
         */
        public TrustedIssuerContext match(Jwt jwt) {
            for (TrustedIssuerContext context : contexts) {
                if (context.matches(jwt)) {
                    return context;
                }
            }
            log.warn("token of trusted issuer {} is valid but matches none of the {} configured entries "
                            + "(token has aud: {}, azp: '{}' - configured are {})",
                    issuerUri, contexts.size(), jwt.getAudience(), jwt.getClaimAsString(CLAIM_AZP),
                    contexts.stream().map(TrustedIssuerContext::describe).collect(Collectors.joining(", ")));
            return null;
        }

        private JwtDecoder buildDecoder() {
            NimbusJwtDecoder result = StringUtils.isNotBlank(jwkSetUri)
                    ? NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build()
                    : NimbusJwtDecoder.withIssuerLocation(issuerUri).build();

            // the entry specific checks are deliberately not part of the decoder: the token is decoded
            // once and matched afterwards, so neither the signature check nor the key set lookup is
            // repeated per configured entry
            result.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                    new JwtTimestampValidator(), new JwtIssuerValidator(issuerUri)));
            return result;
        }
    }

    public static class TrustedIssuerContext {

        private final OAuth2Config.TrustedIssuer config;
        private final OAuth2TokenValidator<Jwt> clientValidator;

        TrustedIssuerContext(OAuth2Config.TrustedIssuer config) {
            this.config = config;
            this.clientValidator = new DelegatingOAuth2TokenValidator<>(clientValidators(config));
        }

        /**
         * whether an already validated token was issued for the client of this entry
         */
        boolean matches(Jwt jwt) {
            return !clientValidator.validate(jwt).hasErrors();
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

        String describe() {
            return "[audience: '" + config.getAudience() + "', authorizedParty: '"
                    + config.getAuthorizedParty() + "']";
        }

        private static List<OAuth2TokenValidator<Jwt>> clientValidators(OAuth2Config.TrustedIssuer config) {
            List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
            if (StringUtils.isNotBlank(config.getAudience())) {
                validators.add(audienceValidator(config.getAudience()));
            }
            if (StringUtils.isNotBlank(config.getAuthorizedParty())) {
                validators.add(new JwtClaimValidator<String>(CLAIM_AZP, config.getAuthorizedParty()::equals));
            }
            return validators;
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
