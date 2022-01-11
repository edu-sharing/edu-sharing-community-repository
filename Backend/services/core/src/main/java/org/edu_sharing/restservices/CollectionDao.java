package org.edu_sharing.restservices;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.PermissionService;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoBaseClient;
import org.edu_sharing.restservices.collection.v1.model.Collection;
import org.edu_sharing.restservices.collection.v1.model.CollectionBaseEntries;
import org.edu_sharing.restservices.collection.v1.model.CollectionReference;
import org.edu_sharing.restservices.node.v1.model.NodeEntries;
import org.edu_sharing.restservices.shared.*;
import org.edu_sharing.service.collection.CollectionService;
import org.edu_sharing.service.collection.CollectionServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.search.model.SortDefinition;
import org.edu_sharing.alfresco.service.toolpermission.ToolPermissionException;
import org.edu_sharing.service.toolpermission.ToolPermissionServiceFactory;

public class CollectionDao {

	public static Logger logger = Logger.getLogger(CollectionDao.class);
	public static final String ROOT = "-root-";

	public enum Scope {
		EDU_ALL,
		EDU_GROUPS,
		MY,
		CUSTOM,
		CUSTOM_PUBLIC
	}
	public enum SearchScope {
		EDU_ALL,
		EDU_GROUPS,
		TYPE_EDITORIAL,
		TYPE_MEDIA_CENTER,
		MY,
		RECENT
	}
	
	private final static String[] PERMISSIONS = new String[] {
			PermissionService.WRITE, PermissionService.DELETE };

	private static final int MAX = 0;


	private final CollectionService collectionClient;

	private final RepositoryDao repoDao;
	private final String collectionId;
	
	private final Collection collection;
	
	private final MCAlfrescoBaseClient baseClient;

	private List<String> access;

	private Preview preview;

	private NodeDao nodeDao;

	public static CollectionDao getCollection(RepositoryDao repoDao, String collectionId)
			throws DAOException {

		try {

			return new CollectionDao(repoDao, collectionId);

		} catch (Exception e) {

			throw DAOException.mapping(e);
		}
	}

	public static CollectionBaseEntries getCollectionsReferences(RepositoryDao repoDao, String parentId, Filter filter, SortDefinition sortDefinition, int skipCount, int maxItems)	throws DAOException {
		try {
			if(parentId==null || parentId.equals(ROOT)){
				throw new IllegalArgumentException("Invalid parameter for parentId");
			}

			return getCollectionsChildren(repoDao, parentId, null, false, filter, Arrays.asList(new String[]{"files"}), sortDefinition, skipCount, maxItems);
		} catch (Exception e) {
			throw DAOException.mapping(e);
		}
	}
	public static CollectionBaseEntries getCollectionsSubcollections(RepositoryDao repoDao, String parentId, SearchScope scope, boolean fetchCounts, Filter filter, SortDefinition sortDefinition, int skipCount, int maxItems)	throws DAOException {
		try {
			return getCollectionsChildren(repoDao, parentId, scope, fetchCounts, filter, Arrays.asList(new String[]{"folders"}), sortDefinition, skipCount, maxItems);
		} catch (Exception e) {
			throw DAOException.mapping(e);
		}
	}
	private static CollectionBaseEntries getCollectionsChildren(RepositoryDao repoDao, String parentId, SearchScope scope, boolean fetchCounts, Filter propFilter, List<String> filter, SortDefinition sortDefinition, int skipCount, int maxItems) throws DAOException {
		try {
			List<Node> result = new ArrayList<>();
			NodeDao parentNode = null;
			if(!ROOT.equals(parentId)) {
				parentNode = NodeDao.getNode(repoDao, parentId);
				parentNode.fetchCounts = false;
			}
			// if this collection is ordered by user, use the position of the elements as primary order criteria
			if (parentNode != null && CCConstants.COLLECTION_ORDER_MODE_CUSTOM.equals(parentNode.asNode().getCollection().getOrderMode())) {
				sortDefinition.addSortDefinitionEntry(
						new SortDefinition.SortDefinitionEntry(CCConstants.getValidLocalName(CCConstants.CCM_PROP_COLLECTION_ORDERED_POSITION), true), 0);
			}

			List<org.alfresco.service.cmr.repository.NodeRef> children =
					repoDao.getCollectionClient().getChildren(
							ROOT.equals(parentId) ? null : parentId,
							scope != null ? scope.toString() : null, sortDefinition, filter);


			//NodeDao.convertAlfrescoNodeRef(repoDao,children)
			NodeEntries sorted = NodeDao.convertToRest(repoDao,
					Filter.createShowAllFilter(),
					NodeDao.convertAlfrescoNodeRef(repoDao, children),
					skipCount,
					maxItems,
					(dao) -> {
						dao.fetchCounts = fetchCounts;
						return dao;
					})
			;
			Pagination pagination = sorted.getPagination();
			for (Node child : sorted.getNodes()) {

				String nodeType = child.getType();

				if (CCConstants.getValidLocalName(CCConstants.CCM_TYPE_MAP).equals(nodeType)) {

					// it's a collection
					result.add(child);

				} else if (CCConstants.getValidLocalName(CCConstants.CCM_TYPE_IO).equals(nodeType)) {

					// it's a reference
					try {
						result.add((CollectionReference) child);
					}catch(ClassCastException e) {
						logger.error("Collection "+parentId+" contains a non-ref object: "+child.getRef().getId()+". Please clean up the collection", e);
					}
				}
			}
			CollectionBaseEntries obj = new CollectionBaseEntries();
			obj.setEntries(result);
			obj.setPagination(pagination);
			return obj;
		}catch(Exception e){
			throw DAOException.mapping(e);
		}
	}

	private String getOrderMode() {
		return this.collection.getOrderMode();
	}

	private CollectionDao(RepositoryDao repoDao, String collectionId) throws DAOException {

		try {
			
			this.collectionClient = repoDao.getCollectionClient();
	
			this.repoDao = repoDao;
			this.collectionId = collectionId;
			this.nodeDao=NodeDao.getNode(repoDao, collectionId);

			this.collection = unmarshalling(repoDao.getId(), collectionClient.get(nodeDao.getStoreIdentifier(),nodeDao.getStoreProtocol(),collectionId, nodeDao.fetchCounts));
			this.baseClient = repoDao.getBaseClient();

		} catch (Exception e) {

			throw DAOException.mapping(e);
		}
	}
	CollectionDao(RepositoryDao repoDao, String collectionId,NodeDao nodeDao,Node node) throws DAOException {

		try {
			
			this.collectionClient = repoDao.getCollectionClient();
	
			this.repoDao = repoDao;
			this.collectionId = collectionId;
			this.nodeDao=nodeDao;
			this.collection = unmarshalling(repoDao.getId(), collectionClient.get(nodeDao.getStoreIdentifier(),nodeDao.getStoreProtocol(),collectionId, nodeDao.fetchCounts));
			this.baseClient = repoDao.getBaseClient();
			this.access = node.getAccess();//baseClient.hasAllPermissions(collectionId, PERMISSIONS);
			this.preview= node.getPreview();

		} catch (Exception e) {

			throw DAOException.mapping(e);
		}
	}
	public String getScope() throws DAOException {
		return collection.getScope();
	}

	public static NodeDao createRoot(RepositoryDao repoDao, Node collection) throws DAOException {

		try {

			org.edu_sharing.service.collection.Collection child = 
					repoDao.getCollectionClient().createAndSetScope(null,marshalling(collection));
			return NodeDao.getNode(repoDao, child.getNodeId(), Filter.createShowAllFilter());

		} catch (Throwable t) {

			throw DAOException.mapping(t);
		}
			
	}

	public NodeDao createChild(Node collection) throws DAOException {

		try {
			org.edu_sharing.service.collection.Collection child = collectionClient.createAndSetScope(collectionId, marshalling(collection));
			return NodeDao.getNode(repoDao, child.getNodeId(),Filter.createShowAllFilter());

		} catch (Throwable t) {

			throw DAOException.mapping(t);
		}
			
	}

	public void update(Node collection) throws DAOException {

		try {
		
			collectionClient.updateAndSetScope(marshalling(collection));
			
		} catch (Exception e) {

			throw DAOException.mapping(e);
		}
		
	}
	
	public void delete() throws DAOException {

		try {
		
			collectionClient.remove(nodeDao.getRef().getId());
			
		} catch (Exception e) {

			throw DAOException.mapping(e);
		}
			
	}
	
	public static synchronized NodeDao addToCollection(RepositoryDao repoDao, String collectionId, String nodeId, String sourceRepositoryId, boolean allowDuplicate) throws DAOException {
		try {

			String resultId=CollectionServiceFactory.getCollectionService(repoDao.getApplicationInfo().getAppId()).
					addToCollection(collectionId,nodeId,sourceRepositoryId,allowDuplicate);
			return NodeDao.getNode(repoDao,resultId,Filter.createShowAllFilter());
		} catch (Throwable t) {

			throw DAOException.mapping(t);
		}
	}
	
	public void removeFromCollection(NodeDao node) throws DAOException {
		
		try {
		
			collectionClient.removeFromCollection(nodeDao.getRef().getId(), node.getRef().getId());
			
		} catch (Exception e) {

			throw DAOException.mapping(e);
		}
			
	}

	public Collection asCollection() throws DAOException {
		return this.collection;
	}

	private List<String> getAccess() {
		return access;
	}

	
	private static org.edu_sharing.service.collection.Collection marshalling(Node node) {
		
		if (node == null) {
			return null;
		}
		
		org.edu_sharing.service.collection.Collection result = new org.edu_sharing.service.collection.Collection();
		
		result.setColor(node.getCollection().getColor());
		result.setDescription(node.getCollection().getDescription());
		result.setLevel0(node.getCollection().isLevel0());
		
		if (node.getRef() != null) {
			
			result.setNodeId(node.getRef().getId());
		}
		
		result.setTitle(node.getTitle());
		result.setType(node.getCollection().getType());
		result.setViewtype(node.getCollection().getViewtype());
		result.setX(node.getCollection().getX());
		result.setY(node.getCollection().getY());
		result.setZ(node.getCollection().getZ());
		result.setPinned(node.getCollection().isPinned());
		result.setAuthorFreetext(node.getCollection().getAuthorFreetext());
		
		if (node.getRef() != null) {
			result.setNodeId(node.getRef().getId());
		}
		result.setScope(node.getCollection().getScope());
		
		return result;
	}
	
	private static Collection unmarshalling(String repoId, org.edu_sharing.service.collection.Collection collection) {
		
		if (collection == null) {
			return null;
		}
		
		Collection result = new Collection();
		
		result.setColor(collection.getColor());
		result.setDescription(collection.getDescription());
		result.setLevel0(collection.isLevel0());
		result.setFromUser(collection.isFromUser());
		
		result.setChildCollectionsCount(collection.getChildCollectionsCount());
		result.setChildReferencesCount(collection.getChildReferencesCount());
		
		result.setTitle(collection.getTitle());
		result.setType(collection.getType());
		result.setOrderMode(collection.getOrderMode());
		result.setViewtype(collection.getViewtype());
		result.setX(collection.getX());
		result.setY(collection.getY());
		result.setZ(collection.getZ());
		result.setPinned(collection.isPinned());
		result.setAuthorFreetext(collection.getAuthorFreetext());

		result.setScope(collection.getScope());

		return result;
	}
	
	public void writePreviewImage(InputStream is, String mimeType) throws DAOException{
		try{
			collectionClient.writePreviewImage(collectionId,is,mimeType);
			//thumbnailService.createThumbnail(ref, QName.createQName(CCConstants.CCM_PROP_MAP_ICON), ,"collection");
		}catch(Exception e){
			throw new DAOException(e,collectionId);
		}
	}

	public void removePreviewImage() throws DAOException{
		try{
			collectionClient.removePreviewImage(collectionId);
		}catch(Exception e){
			throw new DAOException(e,collectionId);
		}
	}

	public static void setPinned(RepositoryDao repoDao, String[] collections) {
		if(!ToolPermissionServiceFactory.getInstance().hasToolPermission(CCConstants.CCM_VALUE_TOOLPERMISSION_COLLECTION_PINNING))
			throw new ToolPermissionException(CCConstants.CCM_VALUE_TOOLPERMISSION_COLLECTION_PINNING);
		AuthenticationUtil.runAsSystem(new RunAsWork<Void>() {
			@Override
			public Void doWork() throws Exception {
				repoDao.getCollectionClient().setPinned(collections);
				return null;
			}
		});
	}

	public void setOrder(String[] nodes) {
		collectionClient.setOrder(collectionId,nodes);
	}

	public Node asNode() throws DAOException {
		return nodeDao.asNode();
	}

	public void addFeedback(HashMap<String, String[]> feedbackData) throws DAOException {
		try {
			collectionClient.addFeedback(nodeDao.getRef().getId(), feedbackData);
		}catch(Throwable t){
			throw new DAOException(t,collectionId);
		}
	}
	public List<CollectionFeedback> getFeedbacks() throws DAOException {
		try {
			return collectionClient.getFeedbacks(nodeDao.getRef().getId()).stream().map((id)->{
				try {
					NodeDao node=NodeDao.getNode(repoDao,id);
					String data = NodeServiceHelper.getProperty(new org.alfresco.service.cmr.repository.NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, id), CCConstants.CCM_PROP_COLLECTION_FEEDBACK_DATA);
					String authority = NodeServiceHelper.getProperty(new org.alfresco.service.cmr.repository.NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, id), CCConstants.CCM_PROP_COLLECTION_FEEDBACK_AUTHORITY);
					CollectionFeedback feedback = new CollectionFeedback();
					feedback.setCreatedAt(node.getCreatedAt());
					if(authority!=null) {
						feedback.setCreator(String.valueOf(Math.abs(authority.hashCode()) % 1000));
					}
					feedback.setFeedback(new Gson().fromJson(data,Map.class));
					return feedback;
				} catch (DAOException e) {
					throw new RuntimeException(e);
				}

			}).collect(Collectors.toList());
		}catch(Throwable t){
			throw new DAOException(t,collectionId);
		}
	}

}
