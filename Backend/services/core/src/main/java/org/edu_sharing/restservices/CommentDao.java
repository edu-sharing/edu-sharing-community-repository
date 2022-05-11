package org.edu_sharing.restservices;

import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.lang.NotImplementedException;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.restservices.comment.v1.model.Comment;
import org.edu_sharing.restservices.comment.v1.model.Comments;
import org.edu_sharing.restservices.shared.UserSimple;
import org.edu_sharing.service.comment.CommentService;
import org.edu_sharing.service.comment.CommentServiceFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CommentDao {
	
	private RepositoryDao repoDao;
	private CommentService commentService;
	public CommentDao(RepositoryDao repoDao) {
		this.repoDao = repoDao;
		this.commentService = CommentServiceFactory.getCommentService(repoDao.getId());
	}
	public void addComment(String nodeId,String commentReference,String comment) throws DAOException{
		try{
			this.commentService.addComment(nodeId, commentReference, comment);	
		}catch(Exception e){
			throw DAOException.mapping(e);
		}
	}
	public Comments getComments(String nodeId) throws DAOException {
		try{
			List<ChildAssociationRef> refs = this.commentService.getComments(nodeId);
			if(refs == null){
				return null;
			}
			List<Comment> comments=new ArrayList<>();
			for(ChildAssociationRef ref : refs) {
				NodeDao node=NodeDao.getNode(repoDao, ref.getChildRef().getId());
				Comment comment=new Comment();
				comment.setRef(node.getRef());
				try {
					PersonDao person=PersonDao.getPerson(repoDao, (String) node.getNativeProperties().get(CCConstants.CM_PROP_C_CREATOR));
					comment.setCreator(person.asPersonSimple());
				}catch(Throwable t) {
					comment.setCreator(UserSimple.getDummy((String) node.getNativeProperties().get(CCConstants.CM_PROP_C_CREATOR)));
				}
				if(node.getNativeProperties().containsKey(CCConstants.CCM_PROP_COMMENT_REPLY)) {
					comment.setReplyTo(
						new org.edu_sharing.restservices.shared.NodeRef(repoDao,new NodeRef((String) node.getNativeProperties().get(CCConstants.CCM_PROP_COMMENT_REPLY)).getId())
						);
				}

				comment.setCreated(node.getCreatedAt().getTime());
				comment.setComment((String) node.getNativeProperties().get(CCConstants.CCM_PROP_COMMENT_CONTENT));
				comments.add(comment);
			}
			Collections.sort(comments,new Comparator<Comment>() {
				@Override
				public int compare(Comment o1, Comment o2) {
					return Long.compare(o1.getCreated(),o2.getCreated());
				}
			});
			Comments commentsObject=new Comments();
			commentsObject.setComments(comments);
			return commentsObject;
		}catch(Exception e){
			throw DAOException.mapping(e);
		}
	}
	public void editComment(String commentId, String comment) throws DAOException {
		try{
			this.commentService.editComment(commentId, comment);	
		}catch(Exception e){
			throw DAOException.mapping(e);
		}
	}
	public void deleteComment(String commentId) throws DAOException {
		try{
			this.commentService.deleteComment(commentId);	
		}catch(Exception e){
			throw DAOException.mapping(e);
		}
	}
}
