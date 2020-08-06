package org.edu_sharing.service.comment;

import java.util.HashMap;
import java.util.List;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.cache.RepositoryCache;
import org.edu_sharing.service.InsufficientPermissionException;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.permission.PermissionService;
import org.edu_sharing.service.permission.PermissionServiceFactory;
import org.edu_sharing.service.toolpermission.ToolPermissionHelper;

public class CommentServiceImpl implements CommentService{

	private NodeService nodeService;
	private PermissionService permissionService;

	public CommentServiceImpl() {
		this.nodeService=NodeServiceFactory.getLocalService();
		this.permissionService=PermissionServiceFactory.getLocalService();
	}
	
	@Override
	public String addComment(String node,String commentReference, String comment) throws Exception {
		ToolPermissionHelper.throwIfToolpermissionMissing(CCConstants.CCM_VALUE_TOOLPERMISSION_COMMENT_WRITE);
		HashMap<String, Object> props = new HashMap<String,Object>();
		if(commentReference!=null && !commentReference.trim().isEmpty()) {
			props.put(CCConstants.CCM_PROP_COMMENT_REPLY,new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, commentReference));
		}
		props.put(CCConstants.CM_NAME,"childcomment");
		props.put(CCConstants.CCM_PROP_COMMENT_CONTENT,comment);
		boolean permission = permissionService.hasPermission(StoreRef.PROTOCOL_WORKSPACE,StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(),node,CCConstants.PERMISSION_COMMENT);
		if(!permission) {
			throw new InsufficientPermissionException("No permission '"+CCConstants.PERMISSION_COMMENT+"' to add comments to node "+node);
		}
		return AuthenticationUtil.runAsSystem(new RunAsWork<String>() {
			@Override
			public String doWork() throws Exception {
				String nodeId=nodeService.createNodeBasic(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,node,CCConstants.CCM_TYPE_COMMENT,CCConstants.CCM_ASSOC_COMMENT, props);
				permissionService.setPermissions(nodeId,null, true);
				new RepositoryCache().remove(node);
				return nodeId;
			}
		});
	}
	@Override
	public List<ChildAssociationRef> getComments(String node) throws Exception {
		return this.nodeService.getChildrenChildAssociationRefType(node,CCConstants.CCM_TYPE_COMMENT);
	}

	@Override
	public void editComment(String commentId, String comment) {
		throwIfNoComment(commentId);
		HashMap<String, Object> props = new HashMap<String,Object>();
		props.put(CCConstants.CCM_PROP_COMMENT_CONTENT,comment);
		nodeService.updateNodeNative(commentId, props);	
	}

	private void throwIfNoComment(String commentId) {
		if(!nodeService.getType(commentId).equals(CCConstants.CCM_TYPE_COMMENT)){
			throw new IllegalArgumentException("Node "+commentId+" is not a comment");
		}
	}

	@Override
	public void deleteComment(String commentId) {
		throwIfNoComment(commentId);
		new RepositoryCache().remove(nodeService.getPrimaryParent(commentId));
		nodeService.removeNode(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(),commentId);
	}

	
}
