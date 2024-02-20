package org.edu_sharing.restservices;

import org.edu_sharing.alfresco.policy.NodeFileSizeExceededException;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class DAONodeFileSizeExceededException extends DAOException {
    private final NodeFileSizeExceededException cause;

    public DAONodeFileSizeExceededException(NodeFileSizeExceededException cause, String nodeId) {
        super(cause, nodeId);
        this.cause = cause;
    }

    @Override
    public Map<String, Serializable> getDetails() {
        return new HashMap<>() {{
           put("actualSize", cause.getActualSize());
           put("maxSize", cause.getMaxSize());
        }};
    }
}
