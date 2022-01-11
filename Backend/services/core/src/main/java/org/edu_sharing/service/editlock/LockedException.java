package org.edu_sharing.service.editlock;

public class LockedException extends Exception {
	
	LockBy lockBy = null;
	public LockedException(LockBy lockBy) {
		this.lockBy = lockBy;
	}
	
	public LockBy getLockBy() {
		return lockBy;
	}
	
}
