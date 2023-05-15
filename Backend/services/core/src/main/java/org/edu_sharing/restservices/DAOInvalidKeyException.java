package org.edu_sharing.restservices;

public class DAOInvalidKeyException extends DAOException {

    private static final long serialVersionUID = 1L;

    DAOInvalidKeyException(Throwable t) {
        super(t, null);
    }
}
