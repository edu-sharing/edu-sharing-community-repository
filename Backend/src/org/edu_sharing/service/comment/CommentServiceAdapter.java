package org.edu_sharing.service.comment;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.commons.lang.NotImplementedException;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.cache.RepositoryCache;
import org.edu_sharing.service.InsufficientPermissionException;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.permission.PermissionService;
import org.edu_sharing.service.permission.PermissionServiceFactory;

import java.util.HashMap;
import java.util.List;

public class CommentServiceAdapter implements CommentService{

	public CommentServiceAdapter(String appId) {
	}
	
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
}
