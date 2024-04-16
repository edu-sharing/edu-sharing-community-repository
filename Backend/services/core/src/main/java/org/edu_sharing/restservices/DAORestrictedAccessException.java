package org.edu_sharing.restservices;

public class DAORestrictedAccessException extends DAOException {
    public DAORestrictedAccessException(Throwable t, String nodeId) {
        super(t, nodeId);
    }
}
