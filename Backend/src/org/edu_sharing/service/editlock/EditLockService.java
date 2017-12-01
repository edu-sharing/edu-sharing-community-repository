package org.edu_sharing.service.editlock;

import java.util.Collection;

import org.alfresco.service.cmr.repository.NodeRef;
import org.edu_sharing.service.InsufficientPermissionException;

public interface EditLockService {
	
	public void lock(NodeRef nodeRef) throws LockedException,InsufficientPermissionException;
	
	public void unlock(NodeRef nodeRef) throws LockedException;
	
	public boolean isLocked(NodeRef nodeRef);
	
	public boolean isLockedByAnotherUser(NodeRef nodeRef);
	
	public LockBy getLock(NodeRef nodeRef);
	
	public void unlockBySession(String sessionId);

	public Collection<NodeRef> getActiveLocks();
	
}
