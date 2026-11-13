package org.edu_sharing.restservices;

import org.edu_sharing.alfresco.policy.NodeActiveContentValidationException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class DAOActiveContentVerificationException extends DAOException {
    private final NodeActiveContentValidationException cause;

    public DAOActiveContentVerificationException(NodeActiveContentValidationException cause, String nodeId) {
        super(cause, nodeId);
        this.cause = cause;
    }

    @Override
    public Map<String, Serializable> getDetails() {
        HashMap<String, Serializable> details = new HashMap<>();
        details.put("findings", new ArrayList<>(cause.getFindings().stream().map(Enum::name).toList()));
        return details;
    }
}
