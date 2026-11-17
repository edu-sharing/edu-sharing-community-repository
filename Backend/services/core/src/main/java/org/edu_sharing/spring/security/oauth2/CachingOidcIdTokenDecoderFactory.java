package org.edu_sharing.spring.security.oauth2;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.oauth2.client.oidc.authentication.OidcIdTokenDecoderFactory;
import org.springframework.security.oauth2.client.oidc.authentication.OidcIdTokenValidator;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoderFactory;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Replacement for spring's {@link OidcIdTokenDecoderFactory} used to decode/validate OIDC id tokens.
 * <p>
 * Since spring security 7 the default factory
 * <ul>
 *     <li>no longer caches the created decoders: every login builds a new {@link NimbusJwtDecoder}
 *     with an empty JWK set cache, so the JWK set of the IdP is fetched on every single login</li>
 *     <li>uses fixed nimbus http timeouts of 500ms for fetching the JWK set, which leads to
 *     sporadic {@code invalid_id_token} errors ("Read timed out") with slower IdPs</li>
 * </ul>
 * This factory caches one decoder per {@link ClientRegistration} instance (restoring the spring security 6
 * behaviour, i.e. the nimbus JWK set cache with 5 minutes ttl is effective again) and uses configurable timeouts.
 * <p>
 * The cache is keyed by the registration instance (ClientRegistration does not override equals) in a
 * {@link WeakHashMap}: the ClientRegistrationRepository is refresh scoped, so after a config reload new
 * registration instances lead to new decoders and the outdated ones are garbage collected.
 * <p>
 * Validation and claim conversion are the same as in spring's default factory (default timestamp validator +
 * {@link OidcIdTokenValidator}, default claim type converter). The jws algorithm is configurable
 * (spring's default: RS256), only asymmetric algorithms verified against the JWK set are supported.
 */
@Slf4j
public class CachingOidcIdTokenDecoderFactory implements JwtDecoderFactory<ClientRegistration> {

    private static final String MISSING_SIGNATURE_VERIFIER_ERROR_CODE = "missing_signature_verifier";

    private final Map<ClientRegistration, JwtDecoder> decoders = Collections.synchronizedMap(new WeakHashMap<>());

    private final SignatureAlgorithm jwsAlgorithm;
    private final Duration connectTimeout;
    private final Duration readTimeout;

    public CachingOidcIdTokenDecoderFactory(SignatureAlgorithm jwsAlgorithm, Duration connectTimeout, Duration readTimeout) {
        this.jwsAlgorithm = jwsAlgorithm;
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
        log.info("oidc id token decoder: jwsAlgorithm={} jwk set connectTimeout={}ms readTimeout={}ms", jwsAlgorithm.getName(), connectTimeout.toMillis(), readTimeout.toMillis());
    }

    /**
     * @param name e.g. RS256, ES256, PS256 (spring's default is RS256)
     * @throws IllegalArgumentException for unknown names and MAC based algorithms (HS256, ...), which are not
     * verified against the JWK set and therefore not supported here
     */
    public static SignatureAlgorithm parseJwsAlgorithm(String name) {
        SignatureAlgorithm algorithm = SignatureAlgorithm.from(name);
        if (algorithm == null) {
            throw new IllegalArgumentException("unsupported jwsAlgorithm '" + name + "' for oidc id token, allowed values: "
                    + Arrays.stream(SignatureAlgorithm.values()).map(SignatureAlgorithm::getName).toList());
        }
        return algorithm;
    }

    @Override
    public JwtDecoder createDecoder(ClientRegistration clientRegistration) {
        return decoders.computeIfAbsent(clientRegistration, this::buildDecoder);
    }

    private JwtDecoder buildDecoder(ClientRegistration clientRegistration) {
        String jwkSetUri = clientRegistration.getProviderDetails().getJwkSetUri();
        if (!StringUtils.hasText(jwkSetUri)) {
            OAuth2Error oauth2Error = new OAuth2Error(MISSING_SIGNATURE_VERIFIER_ERROR_CODE,
                    "Failed to find a Signature Verifier for Client Registration: '"
                            + clientRegistration.getRegistrationId()
                            + "'. Check to ensure you have configured the JwkSet URI.",
                    null);
            throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.toString());
        }
        log.debug("creating id token decoder for registration {} jwkSetUri {}", clientRegistration.getRegistrationId(), jwkSetUri);

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);

        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri)
                .jwsAlgorithm(jwsAlgorithm)
                .restOperations(new RestTemplate(requestFactory))
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithValidators(new OidcIdTokenValidator(clientRegistration)));
        decoder.setClaimSetConverter(OidcIdTokenDecoderFactory.createDefaultClaimTypeConverter());
        return decoder;
    }
}
