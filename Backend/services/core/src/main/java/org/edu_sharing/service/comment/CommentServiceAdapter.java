package org.edu_sharing.service.comment;

import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.lang.NotImplementedException;

import java.util.List;

public class CommentServiceAdapter implements CommentService{


	@Override
	public String addComment(String node,String commentReference, String comment) throws Exception {
		throw new NotImplementedException();
	}
	@Override
	public List<ChildAssociationRef> getComments(String node) throws Exception {
		return null;
	}
	@Override
	public void editComment(String commentId, String comment) {
		throw new NotImplementedException();

	}
	@Override
	public void deleteComment(String commentId) {
		throw new NotImplementedException();

	}

	@Override
	public List<NodeRef> getUsersComments(String userName) {
		return List.of();
	}
}
