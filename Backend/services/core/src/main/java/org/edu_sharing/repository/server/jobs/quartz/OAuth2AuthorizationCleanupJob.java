package org.edu_sharing.repository.server.jobs.quartz;

import lombok.extern.slf4j.Slf4j;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.apache.commons.lang3.StringUtils;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;

/**
 * Removes rows of the table oauth2_authorization of the local authorization server that can not be used
 * anymore.
 *
 * Spring only removes a row when a token is revoked or an authorization code is exchanged, so everything else
 * stays forever: expired tokens, codes that were never exchanged, device flows that were not finished. Each
 * of these rows holds the serialized authentication of the user.
 *
 * A row is removed when every token it carries has expired longer than {@link #gracePeriod} ago - the
 * latest expiry counts, so a row with a valid refresh token is kept even though its access token is long
 * gone. A row without any token is an authorization waiting for the consent of the user, it is removed when
 * it was created longer than {@link #pendingMaxAge} ago (column created_at, added by OAuth2SchemaInitializer).
 */
@Slf4j
@JobDescription(description = "cleanup expired and abandoned authorizations of the local oauth2 authorization server")
public class OAuth2AuthorizationCleanupJob extends AbstractInterruptableJob {

    private static final String DEFAULT_GRACE_PERIOD = "PT1H";
    private static final String DEFAULT_PENDING_MAX_AGE = "P1D";
    private static final int DEFAULT_BATCH_SIZE = 1000;
    private static final int DEFAULT_MAX_ENTRIES = 50000;

    // the spring columns are timestamp without time zone, written through jdbc in the time zone of the jvm,
    // so the cutoff for them is computed here and passed the same way. created_at is a timestamptz filled by
    // the database and is therefore compared on the database side.
    private static final String DELETE_SQL = "DELETE FROM oauth2_authorization WHERE id IN ("
            + "SELECT id FROM oauth2_authorization WHERE "
            + "GREATEST(authorization_code_expires_at, access_token_expires_at, oidc_id_token_expires_at, "
            + "refresh_token_expires_at, user_code_expires_at, device_code_expires_at) < ? "
            + "OR (authorization_code_expires_at IS NULL AND access_token_expires_at IS NULL "
            + "AND oidc_id_token_expires_at IS NULL AND refresh_token_expires_at IS NULL "
            + "AND user_code_expires_at IS NULL AND device_code_expires_at IS NULL "
            + "AND created_at < CURRENT_TIMESTAMP - ? * INTERVAL '1 second') "
            + "LIMIT ?)";

    // only present when the authorization server is enabled, see OAuth2SchemaConfiguration
    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RetryingTransactionHelper retryingTransactionHelper;

    @JobFieldDescription(description = "how long a row is kept after the last of its tokens expired, iso 8601 duration", sampleValue = DEFAULT_GRACE_PERIOD)
    private String gracePeriod;

    @JobFieldDescription(description = "rows without any token (waiting for consent) are removed when created longer than this ago, iso 8601 duration", sampleValue = DEFAULT_PENDING_MAX_AGE)
    private String pendingMaxAge;

    @JobFieldDescription(description = "number of rows removed per transaction")
    private Integer batchSize;

    @JobFieldDescription(description = "maximum number of rows removed in one run, 0 for unlimited")
    private Integer maxEntries;

    @Override
    protected void executeInterruptable(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        if (jdbcTemplate == null) {
            log.info("oauth2 authorization server is disabled, nothing to clean up");
            return;
        }

        Duration grace = Duration.parse(StringUtils.defaultIfBlank(gracePeriod, DEFAULT_GRACE_PERIOD).trim());
        Duration pending = Duration.parse(StringUtils.defaultIfBlank(pendingMaxAge, DEFAULT_PENDING_MAX_AGE).trim());
        int pageSize = (batchSize == null || batchSize < 1) ? DEFAULT_BATCH_SIZE : batchSize;
        int limit = (maxEntries == null || maxEntries < 0) ? DEFAULT_MAX_ENTRIES : maxEntries;

        log.info("starting oauth2 authorization cleanup, gracePeriod: {}, pendingMaxAge: {}", grace, pending);

        Timestamp expiredBefore = Timestamp.from(Instant.now().minus(grace));
        int removed = 0;

        while (!Thread.currentThread().isInterrupted()) {
            int fetch = (limit > 0) ? Math.min(pageSize, limit - removed) : pageSize;
            if (fetch < 1) {
                log.info("reached the limit of {} rows for this run, the rest is handled by the next one", limit);
                break;
            }

            int deleted = retryingTransactionHelper.doInTransaction(() ->
                    jdbcTemplate.update(DELETE_SQL, expiredBefore, pending.toSeconds(), fetch));
            removed += deleted;

            if (deleted < fetch) {
                break;
            }
        }

        log.info("finished oauth2 authorization cleanup, removed {} rows", removed);
    }
}
