package org.edu_sharing.service.altcha;

import com.typesafe.config.Config;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.repo.cache.SimpleCache;
import org.altcha.altcha.v2.Altcha;
import org.apache.commons.lang3.StringUtils;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Creates and verifies ALTCHA (proof-of-work) challenges, used to protect guest-accessible endpoints.
 * <p>
 * The HMAC secret is taken from {@code repository.altcha.secret}. If it is empty, a random secret is generated
 * per instance on startup. In that case a solution is only valid on the instance which issued the challenge,
 * which requires sticky sessions but also keeps the node-local replay cache sufficient.
 */
@Slf4j
@Service
public class AltchaService {

    public static final String HEADER = "X-Altcha";
    public static final String ACTION_REPORT = "report";
    public static final String ALGORITHM = "PBKDF2/SHA-256";

    static final String CONFIG_PATH = "repository.altcha";
    static final String REPLAY_CACHE_BEAN = "eduSharingAltchaReplayCache";

    private static final String DATA_ACTION = "action";
    // a valid base64 payload is far below this size, larger values are rejected without parsing
    private static final int MAX_PAYLOAD_LENGTH = 4096;

    private final Supplier<Config> config;
    private final SimpleCache<String, Long> replayCache;
    private final String secret;

    @Autowired
    @SuppressWarnings("unchecked")
    public AltchaService() {
        this(
                () -> LightbendConfigLoader.get().getConfig(CONFIG_PATH),
                (SimpleCache<String, Long>) AlfAppContextGate.getApplicationContext().getBean(REPLAY_CACHE_BEAN)
        );
    }

    AltchaService(Supplier<Config> config, SimpleCache<String, Long> replayCache) {
        this.config = config;
        this.replayCache = replayCache;
        String configuredSecret = config.get().getString("secret");
        if (StringUtils.isBlank(configuredSecret)) {
            log.info("No {}.secret configured, using a random secret for this instance", CONFIG_PATH);
            this.secret = Altcha.bytesToHex(Altcha.randomBytes(32));
        } else {
            this.secret = configuredSecret;
        }
    }

    public boolean isEnabled() {
        return config.get().getBoolean("enabled");
    }

    /**
     * Create a new signed challenge bound to the given action
     *
     * @param clientAddress remote address of the requesting client, used to determine the cost
     */
    public Altcha.Challenge createChallenge(String action, String clientAddress) throws Exception {
        return Altcha.createChallenge(new Altcha.CreateChallengeOptions()
                .algorithm(ALGORITHM)
                .cost(resolveCost(clientKey(clientAddress)))
                .expiresInSeconds(config.get().getLong("expiresSeconds"))
                .hmacSignatureSecret(secret)
                .data(Map.of(DATA_ACTION, action)));
    }

    /**
     * Cost (PBKDF2 iterations) for a new challenge.
     * Currently static, the client key allows to make it adaptive per client later on.
     *
     * @param clientKey see {@link #clientKey(String)}
     */
    protected int resolveCost(String clientKey) {
        return config.get().getInt("cost");
    }

    /**
     * Verify the payload sent by the client and mark it as used
     *
     * @throws AltchaVerificationException with status 403 if the payload is invalid, expired or bound to another action,
     *                                     with status 409 if the payload was already used
     */
    public void verify(String payload, String action) throws AltchaVerificationException {
        if (StringUtils.isBlank(payload) || payload.length() > MAX_PAYLOAD_LENGTH) {
            throw new AltchaVerificationException(AltchaVerificationException.Reason.INVALID);
        }
        Altcha.Challenge challenge;
        try {
            Altcha.VerifySolutionResult result = Altcha.verifySolution(payload, secret, Altcha.pbkdf2());
            if (!result.verified()) {
                log.debug("ALTCHA verification failed: {}", result);
                throw new AltchaVerificationException(AltchaVerificationException.Reason.INVALID);
            }
            challenge = Altcha.parsePayload(payload).challenge();
        } catch (AltchaVerificationException e) {
            throw e;
        } catch (Exception e) {
            log.debug("ALTCHA payload could not be parsed", e);
            throw new AltchaVerificationException(AltchaVerificationException.Reason.INVALID);
        }
        // parameters are covered by the signature, so the bound action can be trusted
        Map<String, Object> data = challenge.parameters().data();
        if (data == null || !action.equals(data.get(DATA_ACTION))) {
            throw new AltchaVerificationException(AltchaVerificationException.Reason.INVALID);
        }
        markAsUsed(challenge.signature(), challenge.parameters().expiresAt());
    }

    /**
     * Atomically insert the challenge signature into the replay cache.
     * The cache is node-local (cluster.type=local), which is sufficient because solutions are bound to the
     * issuing instance via the per-instance secret. The cache ttl must be at least repository.altcha.expiresSeconds.
     */
    private void markAsUsed(String signature, Long expiresAt) throws AltchaVerificationException {
        synchronized (replayCache) {
            if (replayCache.contains(signature)) {
                throw new AltchaVerificationException(AltchaVerificationException.Reason.REPLAYED);
            }
            replayCache.put(signature, expiresAt);
        }
    }

    /**
     * Key identifying a client for cost calculation: the IPv4 address or the /64 prefix of an IPv6 address
     */
    static String clientKey(String clientAddress) {
        if (StringUtils.isBlank(clientAddress)) {
            return "";
        }
        try {
            byte[] address = InetAddress.getByName(clientAddress).getAddress();
            if (address.length == 16) {
                return Altcha.bytesToHex(Arrays.copyOf(address, 8)) + "/64";
            }
        } catch (Exception e) {
            log.debug("Could not parse client address {}", clientAddress);
        }
        return clientAddress;
    }
}
