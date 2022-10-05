package org.edu_sharing.restservices;

import org.edu_sharing.restservices.DAOException;

public class DAODuplicateNodeException extends DAOException {

    private static final long serialVersionUID = 1L;

    DAODuplicateNodeException(Throwable t, String nodeId) {
        super(t,nodeId);
    }
}
