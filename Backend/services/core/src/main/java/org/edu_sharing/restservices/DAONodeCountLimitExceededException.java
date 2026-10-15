package org.edu_sharing.restservices;

import org.edu_sharing.service.collection.NodeCountLimitExceededException;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class DAONodeCountLimitExceededException extends DAOException {
    private final NodeCountLimitExceededException cause;

    public DAONodeCountLimitExceededException(NodeCountLimitExceededException cause, String nodeId) {
        super(cause, nodeId);
        this.cause = cause;
    }

    @Override
    public Map<String, Serializable> getDetails() {
        return new HashMap<>() {{
            put("maxNodes", cause.getMaxNodes());
        }};
    }
}
