package org.edu_sharing.restservices;

import org.edu_sharing.service.quota.NodeCountQuotaExceededException;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * The count quota (max. number of owned materials / collections / folders) of the user is exceeded
 * Mapped like the size quota ({@link DAOQuotaException}) to HTTP 507
 */
public class DAONodeCountQuotaException extends DAOQuotaException {
    private final NodeCountQuotaExceededException cause;

    public DAONodeCountQuotaException(NodeCountQuotaExceededException cause, String nodeId) {
        super(cause, nodeId);
        this.cause = cause;
    }

    @Override
    public Map<String, Serializable> getDetails() {
        Map<String, Serializable> details = new HashMap<>();
        details.put("quotaType", cause.getType().name());
        details.put("limit", cause.getLimit());
        details.put("current", cause.getCurrent());
        return details;
    }
}
