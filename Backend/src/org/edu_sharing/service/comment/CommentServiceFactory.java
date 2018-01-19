package org.edu_sharing.service.comment;

public class CommentServiceFactory {
	public static CommentService getCommentService(){
		return new CommentServiceImpl();
	}
}
