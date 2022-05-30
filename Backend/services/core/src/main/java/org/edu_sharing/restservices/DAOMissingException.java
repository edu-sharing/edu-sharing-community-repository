package org.edu_sharing.restservices;

public class DAOMissingException extends DAOException {

	private static final long serialVersionUID = 1L;
	public DAOMissingException(Throwable t) {
		super(t,null);
	}
	DAOMissingException(Throwable t, String nodeId) {
		super(t,nodeId);
	}
}
