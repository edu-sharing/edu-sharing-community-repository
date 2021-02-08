package org.edu_sharing.restservices;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.domain.node.NodeExistsException;
import org.alfresco.repo.security.authentication.AuthenticationException;
import org.alfresco.repo.security.permissions.AccessDeniedException;
import org.alfresco.service.cmr.repository.DuplicateChildNodeNameException;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.InvalidStoreRefException;
import org.alfresco.service.cmr.security.NoSuchPersonException;
import org.alfresco.service.cmr.usage.ContentQuotaException;
import org.edu_sharing.service.InsufficientPermissionException;
import org.edu_sharing.service.NotAnAdminException;
import org.edu_sharing.service.collection.DuplicateNodeException;
import org.edu_sharing.service.permission.PermissionException;
import org.edu_sharing.alfresco.service.toolpermission.ToolPermissionException;
import org.edu_sharing.service.permission.RestrictedAccessException;

public class DAOException extends Exception {

	private static final long serialVersionUID = 1L;
	private String nodeId;

	protected DAOException(Throwable t, String nodeId) {
		super(t);
		this.nodeId=nodeId;
	}
	
	@Override
	public String toString() {
		return super.toString()+addNodeId();
	}
	private String addNodeId() {
		return (nodeId==null ? "" : " at node "+nodeId);
	}
	
	@Override
	public String getMessage() {
		return super.getMessage();
	}
	
	public static DAOException mapping(Throwable t,String nodeId) {
		
		if (t instanceof DAOException) {
			
			return (DAOException) t;
		}
		
		if (   t instanceof AccessDeniedException
				|| t instanceof AuthenticationException || t instanceof PermissionException
				|| t instanceof InsufficientPermissionException || t instanceof NotAnAdminException) {
			
			return new DAOSecurityException(t,nodeId); 
		}
		if (t instanceof ContentQuotaException){
			return new DAOQuotaException(t,nodeId);
		}
		if(t instanceof AlfrescoRuntimeException
				&& t.getCause() != null
				&& t.getCause().getClass().getName().contains("VirusDetectedException")){
			return new DAOVirusDetectedException(t.getCause(),nodeId);
		}
		if (t instanceof NodeExistsException) {
			
			return new DAOValidationException(t,nodeId); 
		}
		if(t instanceof ToolPermissionException){
			return new DAOToolPermissionException(t);
		}
		if(t instanceof RestrictedAccessException){
			return new DAORestrictedAccessException(t,nodeId);
		}
		if(t instanceof DuplicateChildNodeNameException || t instanceof DuplicateNodeException){
			return new DAODuplicateNodeNameException(t,nodeId);
		}

		if (   t instanceof NoSuchPersonException
			|| t instanceof InvalidStoreRefException
			|| t instanceof InvalidNodeRefException) {
			
			return new DAOMissingException(t,nodeId); 
		}
		
		return new DAOException(t,nodeId);
	}

	public static DAOException mapping(Throwable t) {
		return mapping(t,null);
	}
}
