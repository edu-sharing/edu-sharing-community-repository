package org.edu_sharing.restservices;

public class DAODuplicateNodeNameException extends DAOException {

	private static final long serialVersionUID = 1L;

	DAODuplicateNodeNameException(Throwable t, String nodeId) {
		super(t,nodeId);
	}
}
