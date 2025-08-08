package org.edu_sharing.service.share;

import org.alfresco.service.cmr.repository.NodeRef;
import org.edu_sharing.repository.client.rpc.Share;

public interface GlobalShareService {

	long EXPIRY_DATE_UNLIMITED = - 1;
	String EMAIL_TYPE_LINK = "LINK";
	
	Share createShare(String nodeId, long expiryDate, String password) throws EMailValidationException, EMailSendFailedException, ExpiryDateValidationException, NodeDoesNotExsistException, PermissionFailedException;
	String createShare(String nodeId, String[] emails, long expiryDate, String password, String emailMessageLocale) throws EMailValidationException, EMailSendFailedException, ExpiryDateValidationException, NodeDoesNotExsistException, PermissionFailedException;
	
	void updateShare(String nodeId, String email, long expiryDate) throws EMailValidationException, ExpiryDateValidationException, NodeDoesNotExsistException, PermissionFailedException;
	
	void updateShare(Share share);
	
	void removeShare(String nodeId, String shareNodeId);
	
	Share[] getShares(String nodeId);
	
	Share getShare(String nodeId, String token);

	void updateDownloadCount(Share share);

	boolean isNodeAccessibleViaShare(NodeRef sharedNode, String accessNodeId);
}
