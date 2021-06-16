package org.edu_sharing.service.share;

import org.alfresco.service.cmr.repository.NodeRef;
import org.edu_sharing.repository.client.rpc.Share;

public interface ShareService {

	public static final long EXPIRY_DATE_UNLIMITED = - 1;
	public static final String EMAIL_TYPE_LINK = "LINK";
	
	public Share createShare(String nodeId, long expiryDate, String password) throws EMailValidationException, EMailSendFailedException, ExpiryDateValidationException, NodeDoesNotExsistException, PermissionFailedException;
	public String createShare(String nodeId, String[] emails, long expiryDate, String password, String emailMessageLocale) throws EMailValidationException, EMailSendFailedException, ExpiryDateValidationException, NodeDoesNotExsistException, PermissionFailedException;
	
	public void updateShare(String nodeId, String email, long expiryDate) throws EMailValidationException, ExpiryDateValidationException, NodeDoesNotExsistException, PermissionFailedException;
	
	public void updateShare(Share share);
	
	public void removeShare(String shareNodeId);
	
	public Share[] getShares(String nodeId);
	
	public Share getShare(String nodeId, String token);

	public void updateDownloadCount(Share share);

	boolean isNodeAccessibleViaShare(NodeRef sharedNode, String accessNodeId);
}
