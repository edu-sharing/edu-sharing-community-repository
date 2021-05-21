package org.edu_sharing.service.comment;

import java.util.List;

import org.alfresco.service.cmr.repository.ChildAssociationRef;

public interface CommentService {

	String addComment(String node, String commentReference, String comment) throws Exception;

	List<ChildAssociationRef> getComments(String node) throws Exception;

	void editComment(String commentId, String comment);	
	void deleteComment(String commentId);	
}
