package org.edu_sharing.service;

public class InsufficientPermissionException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	
	
	public InsufficientPermissionException(String message) {
		super(message);
	}
}
