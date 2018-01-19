package org.edu_sharing.service.comment;

import java.util.Collection;
import java.util.List;

import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.edu_sharing.service.InsufficientPermissionException;
import org.edu_sharing.service.config.model.Config;
import org.edu_sharing.service.config.model.Values;

public interface CommentService {

	String addComment(String node, String commentReference, String comment) throws Exception;

	List<ChildAssociationRef> getComments(String node) throws Exception;

	void editComment(String commentId, String comment);	
	void deleteComment(String commentId);	
}
