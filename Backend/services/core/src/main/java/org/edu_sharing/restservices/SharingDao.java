package org.edu_sharing.restservices;

import org.edu_sharing.repository.client.rpc.Share;
import org.alfresco.service.cmr.repository.StoreRef;
import org.edu_sharing.restservices.shared.*;
import org.edu_sharing.restservices.sharing.v1.model.SharingInfo;
import org.edu_sharing.restservices.node.v1.model.SignedNodeEntry;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;

import org.edu_sharing.service.InsufficientPermissionException;
import org.edu_sharing.service.share.GlobalShareService;
import org.edu_sharing.service.share.GlobalShareServiceImpl;
import org.edu_sharing.spring.ApplicationContextFactory;

import java.util.*;

public class SharingDao {
	public static SharingInfo getInfo(RepositoryDao repositoryDao, String node, String token,String password) throws DAOException{
		try {
			Share share = getShare(node, token);
			return new SharingInfo(share, NodeDao.getNode(repositoryDao, node),password);
		}catch(Throwable t){
			throw DAOException.mapping(t);

		}
	}

	private static Share getShare(String node, String token) {
		GlobalShareService service = ApplicationContextFactory.getApplicationContext().getBean(GlobalShareService.class);
		Share share = service.getShare(node, token);
		if (share == null)
			throw new IllegalArgumentException("Share with token " + token + " does not exist");
		return share;
	}

	/**
	 * Builds the data the rendering service needs to display a node that is accessed via a link share,
	 * see {@code NodeApi#getMetadataSigned} for the regular, logged in variant.
	 * <p>
	 * The share has to allow viewing. The permissions of the token (and of the signed node) are limited
	 * to what the share grants, independent of the user the node is read with.
	 *
	 * @param childId optional id of a node below the shared node (e.g. an element of a shared folder)
	 */
	public static SignedNodeEntry getSignedNode(RepositoryDao repositoryDao, String node, String token, String password, String childId) throws DAOException {
		try {
			Share share = getShare(node, token);
			if (share.getExpiryDate() != GlobalShareService.EXPIRY_DATE_UNLIMITED && new Date().after(new Date(share.getExpiryDate()))) {
				throw new InsufficientPermissionException("Share expired");
			}
			if (share.getPassword() != null && !share.getPassword().equals(GlobalShareServiceImpl.encryptPassword(password))) {
				throw new InsufficientPermissionException("Invalid password supplied");
			}
			if (!share.isViewAllowed()) {
				throw new InsufficientPermissionException("The share does not allow to view the content");
			}
			String targetId = childId == null || childId.isEmpty() ? node : childId;
			if (!targetId.equals(node)) {
				GlobalShareService service = ApplicationContextFactory.getApplicationContext().getBean(GlobalShareService.class);
				if (!service.isNodeAccessibleViaShare(new org.alfresco.service.cmr.repository.NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, node), targetId)) {
					throw new InsufficientPermissionException("The node is not part of the share");
				}
			}

			NodeDao nodeDao = NodeDao.getNode(repositoryDao, targetId);
			// resolve the additional, dynamic links for the rs2
			nodeDao.resolveAdditionalLinks = true;
			Node nodeConverted = nodeDao.asNode();
			// the node was read as system, so its access must not leak into the signed data
			List<String> permissions = NodeDao.getSharePermissions(share.isDownloadAllowed());
			nodeConverted.setAccess(permissions);
			nodeConverted.setAccessEffective(permissions);

			SignedNode signedNode = NodeDao.getSignedNode(nodeConverted);
			Base64.Encoder encoder = Base64.getEncoder();
			SignedNodeEntry response = new SignedNodeEntry();
			response.setNode(nodeConverted);
			response.setJwt(NodeDao.getShareJWT(nodeConverted, permissions));
			response.setSignedNode(encoder.encodeToString(signedNode.getNode().getBytes()));
			response.setSignature(encoder.encodeToString(signedNode.getSignature()));
			response.setSignatureAlgorithm(signedNode.getSignatureAlgorithm());
			ApplicationInfo rs = ApplicationInfoList.getRenderingService2();
			if (rs != null) {
				response.setRenderingBaseUrl(rs.getContentUrl());
			}
			return response;
		} catch (Throwable t) {
			throw DAOException.mapping(t);
		}
	}

	public static List<NodeRef> getChildren(RepositoryDao repositoryDao, String node, String token, String password) throws DAOException {
		try {
			Share share = getShare(node, token);
			if(share.getPassword()!=null && !share.getPassword().equals(GlobalShareServiceImpl.encryptPassword(password))){
				throw new InsufficientPermissionException("Invalid password supplied");
			}
			NodeDao nodeDao = NodeDao.getNode(repositoryDao, node);
			if(!nodeDao.isDirectory())
				throw new IllegalArgumentException("Node "+node+" is not a directory");
			return nodeDao.getChildren();
		}catch(Throwable t){
			throw DAOException.mapping(t);
		}
	}
}
