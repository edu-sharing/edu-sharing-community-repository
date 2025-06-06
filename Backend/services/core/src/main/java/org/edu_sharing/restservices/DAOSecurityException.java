package org.edu_sharing.restservices;

public class DAOSecurityException extends DAOException {

	private static final long serialVersionUID = 1L;

	DAOSecurityException(Throwable t, String nodeId) {
		super(t,nodeId);
	}
	public DAOSecurityException(Throwable t) {
		super(t,null);
	}
}
