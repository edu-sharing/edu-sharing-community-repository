package org.edu_sharing.service.quota;

import lombok.Getter;

/**
 * Thrown if a user would own more nodes of the given type than allowed by the person's count quota
 */
@Getter
public class NodeCountQuotaExceededException extends RuntimeException {
    private final NodeCountQuotaType type;
    private final long limit;
    private final long current;

    public NodeCountQuotaExceededException(NodeCountQuotaType type, long limit, long current) {
        super("The count quota for " + type.name().toLowerCase() + " is exceeded (" + current + " / " + limit + ")");
        this.type = type;
        this.limit = limit;
        this.current = current;
    }
}
