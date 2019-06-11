package org.edu_sharing.restservices;

public class DAOQuotaException extends DAOException {
    public DAOQuotaException(Throwable t, String nodeId) {
        super(t,nodeId);
    }
}
