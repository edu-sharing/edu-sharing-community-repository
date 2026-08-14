package org.edu_sharing.restservices;

import java.util.Collection;

/**
 * Request size limits for the bulk endpoints of the iam api.
 * <p>
 * All of these endpoints handle the complete list within a single transaction, so a single
 * failing entry rolls back the whole batch. The limits are therefore a property of the
 * implementation and are intentionally not configurable - raising them does not make the
 * rollback of a large batch any cheaper.
 * <p>
 * The constants are compile time constants so that they can be referenced from the
 * {@code @Operation} descriptions of the endpoints, keeping documentation and enforcement in sync.
 */
public class BulkRequestLimits {

    /**
     * maximum number of authorities accepted in a single bulk request of the iam api
     */
    public static final int IAM_MAX_BATCH_SIZE = 1000;

    /**
     * maximum number of users accepted in a single bulk request of the iam api that sends
     * notification mails. mails are sent synchronously and sequentially, one smtp roundtrip
     * per user, so this limit is considerably lower.
     */
    public static final int IAM_MAX_BATCH_SIZE_NOTIFY = 100;

    private BulkRequestLimits() {
    }

    /**
     * validates the size of the authority list of a bulk iam request
     *
     * @param authorities the authorities sent by the client
     * @param notify      whether the request also triggers notification mails
     * @throws DAOValidationException if the list is empty or exceeds the limit (mapped to http 400)
     */
    public static void checkIamBatchSize(Collection<String> authorities, boolean notify) throws DAOValidationException {
        if (authorities == null || authorities.isEmpty()) {
            throw new DAOValidationException(new IllegalArgumentException("no authorities given"));
        }

        int limit = notify ? IAM_MAX_BATCH_SIZE_NOTIFY : IAM_MAX_BATCH_SIZE;
        if (authorities.size() > limit) {
            throw new DAOValidationException(new IllegalArgumentException(
                    "too many authorities in a single request: " + authorities.size() + ", maximum is " + limit
                            + (notify ? " when notification mails are enabled" : "")
                            + ". please split the request into multiple batches."));
        }
    }
}
