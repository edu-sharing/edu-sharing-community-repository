package org.edu_sharing.restservices;

public class DAOValidationException extends DAOException {

	private static final long serialVersionUID = 1L;

	DAOValidationException(Throwable t, String nodeId) {
		super(t,nodeId);
	}
	DAOValidationException(Throwable t) {
		super(t,null);
	}
}
