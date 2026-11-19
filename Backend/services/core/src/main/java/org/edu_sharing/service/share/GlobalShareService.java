package org.edu_sharing.service.share;

import org.alfresco.service.cmr.repository.NodeRef;
import org.edu_sharing.repository.client.rpc.Share;

public interface GlobalShareService {

	long EXPIRY_DATE_UNLIMITED = - 1;
	String EMAIL_TYPE_LINK = "LINK";
	
	/**
	 * Creates a link share.
	 *
	 * @param view     whether the recipient may view the content in the browser, {@code null} for the default of the current context
	 * @param download whether the recipient may download the content, {@code null} for the default of the current context
	 * @throws IllegalArgumentException if neither view nor download would be allowed
	 */
	Share createShare(String nodeId, long expiryDate, String password, Boolean view, Boolean download) throws EMailValidationException, EMailSendFailedException, ExpiryDateValidationException, NodeDoesNotExsistException, PermissionFailedException;
	String createShare(String nodeId, String[] emails, long expiryDate, String password, String emailMessageLocale) throws EMailValidationException, EMailSendFailedException, ExpiryDateValidationException, NodeDoesNotExsistException, PermissionFailedException;
	
	void updateShare(String nodeId, String email, long expiryDate) throws EMailValidationException, ExpiryDateValidationException, NodeDoesNotExsistException, PermissionFailedException;
	
	void updateShare(Share share);
	
	void removeShare(String nodeId, String shareNodeId);
	
	Share[] getShares(String nodeId);

	/**
	 * same as {@link #getShares(String)}, but for a node in a store other than the default workspace
	 * (e.g. the archive/trashcan store)
	 */
	Share[] getShares(NodeRef nodeRef);
	
	Share getShare(String nodeId, String token);

	void updateDownloadCount(Share share);

	boolean isNodeAccessibleViaShare(NodeRef sharedNode, String accessNodeId);
}
