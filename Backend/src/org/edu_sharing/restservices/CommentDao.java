package org.edu_sharing.restservices;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.edu_sharing.metadataset.v2.MetadataGroup;
import org.edu_sharing.metadataset.v2.MetadataList;
import org.edu_sharing.metadataset.v2.MetadataReaderV2;
import org.edu_sharing.metadataset.v2.MetadataSearchHelper;
import org.edu_sharing.metadataset.v2.MetadataSetInfo;
import org.edu_sharing.metadataset.v2.MetadataSetV2;
import org.edu_sharing.metadataset.v2.MetadataTemplate;
import org.edu_sharing.metadataset.v2.MetadataWidget;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.RepoFactory;
import org.edu_sharing.restservices.comment.v1.model.Comment;
import org.edu_sharing.restservices.comment.v1.model.Comments;
import org.edu_sharing.restservices.mds.v1.model.GroupV2;
import org.edu_sharing.restservices.mds.v1.model.ListV2;
import org.edu_sharing.restservices.mds.v1.model.Suggestions;
import org.edu_sharing.restservices.mds.v1.model.ViewV2;
import org.edu_sharing.restservices.mds.v1.model.WidgetV2;
import org.edu_sharing.restservices.shared.MdsV2;
import org.edu_sharing.service.comment.CommentService;
import org.edu_sharing.service.comment.CommentServiceFactory;

import com.google.gwt.user.client.ui.SuggestOracle;

public class CommentDao {
	
	private RepositoryDao repoDao;
	private CommentService commentService;
	public CommentDao(RepositoryDao repoDao) {
		this.repoDao = repoDao;		
		this.commentService=CommentServiceFactory.getCommentService();
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
			List<Comment> comments=new ArrayList<>();
			for(ChildAssociationRef ref : refs) {
				NodeDao node=NodeDao.getNode(repoDao, ref.getChildRef().getId());
				Comment comment=new Comment();
				comment.setRef(node.getRef());
				try {
					PersonDao person=PersonDao.getPerson(repoDao, (String) node.getNativeProperties().get(CCConstants.CM_PROP_C_CREATOR));
					comment.setCreator(person.asPersonSimple());
				}catch(Throwable t) {}
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
