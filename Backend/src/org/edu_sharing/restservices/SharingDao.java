package org.edu_sharing.restservices;

import io.swagger.util.Json;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.DuplicateChildNodeNameException;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.QName;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.metadataset.v2.MetadataQuery;
import org.edu_sharing.metadataset.v2.MetadataQueryParameter;
import org.edu_sharing.repository.client.rpc.Notify;
import org.edu_sharing.repository.client.rpc.Share;
import org.edu_sharing.repository.client.rpc.User;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.metadata.ValueTool;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.repository.server.tools.ImageTool;
import org.edu_sharing.repository.server.tools.NameSpaceTool;
import org.edu_sharing.repository.server.tools.cache.PreviewCache;
import org.edu_sharing.restservices.collection.v1.model.Collection;
import org.edu_sharing.restservices.node.v1.model.NodeShare;
import org.edu_sharing.restservices.node.v1.model.NotifyEntry;
import org.edu_sharing.restservices.node.v1.model.WorkflowHistory;
import org.edu_sharing.restservices.shared.*;
import org.edu_sharing.restservices.shared.NodeSearch.Facette;
import org.edu_sharing.restservices.shared.NodeSearch.Facette.Value;
import org.edu_sharing.restservices.sharing.v1.model.SharingInfo;

import org.edu_sharing.service.InsufficientPermissionException;
import org.edu_sharing.service.license.LicenseService;
import org.edu_sharing.service.mime.MimeTypesV2;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.notification.NotificationServiceFactory;
import org.edu_sharing.service.permission.PermissionServiceFactory;
import org.edu_sharing.service.permission.PermissionServiceHelper;
import org.edu_sharing.service.remote.RemoteObjectService;
import org.edu_sharing.service.search.SearchService;
import org.edu_sharing.service.search.SearchServiceFactory;
import org.edu_sharing.service.search.model.SearchToken;
import org.edu_sharing.service.search.model.SortDefinition;
import org.edu_sharing.service.search.model.SortDefinition.SortDefinitionEntry;
import org.edu_sharing.service.share.ShareService;
import org.edu_sharing.service.share.ShareServiceImpl;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.*;
import java.util.Map.Entry;

public class SharingDao {
	public static SharingInfo getInfo(RepositoryDao repositoryDao, String node, String token,String password) throws DAOException{
		try {
			Share share = getShare(node, token);
			return new SharingInfo(share, NodeDao.getNode(repositoryDao, node).asNode(),password);
		}catch(Throwable t){
			throw DAOException.mapping(t);

		}
	}

	private static Share getShare(String node, String token) {
		ShareService service = new ShareServiceImpl();
		Share share = service.getShare(node, token);
		if (share == null)
			throw new IllegalArgumentException("Share with token " + token + " does not exist");
		return share;
	}

	public static List<NodeRef> getChildren(RepositoryDao repositoryDao, String node, String token, String password) throws DAOException {
		try {
			Share share = getShare(node, token);
			if(share.getPassword()!=null && !share.getPassword().equals(ShareServiceImpl.encryptPassword(password))){
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
