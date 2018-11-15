package org.edu_sharing.restservices;

import java.io.InputStream;
import java.util.*;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.thumbnail.ThumbnailService;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.MCAlfrescoBaseClient;
import org.edu_sharing.repository.server.tools.ImageTool;
import org.edu_sharing.repository.server.tools.cache.PreviewCache;
import org.edu_sharing.restservices.collection.v1.model.Collection;
import org.edu_sharing.restservices.collection.v1.model.CollectionBase;
import org.edu_sharing.restservices.collection.v1.model.CollectionBaseEntries;
import org.edu_sharing.restservices.collection.v1.model.CollectionReference;
import org.edu_sharing.restservices.node.v1.model.NodeEntries;
import org.edu_sharing.restservices.shared.*;
import org.edu_sharing.service.Constants;
import org.edu_sharing.service.collection.CollectionService;
import org.edu_sharing.service.search.model.SortDefinition;
import org.edu_sharing.service.toolpermission.ToolPermissionException;
import org.edu_sharing.service.toolpermission.ToolPermissionServiceFactory;
import org.springframework.context.ApplicationContext;

import com.google.gwt.i18n.server.testing.Child;

public class CollectionDao {

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
		MY
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

			return getCollectionsChildren(repoDao, parentId, null, filter, Arrays.asList(new String[]{"files"}), sortDefinition, skipCount, maxItems);
		} catch (Exception e) {
			throw DAOException.mapping(e);
		}
	}
	public static CollectionBaseEntries getCollectionsSubcollections(RepositoryDao repoDao, String parentId, SearchScope scope, Filter filter, SortDefinition sortDefinition, int skipCount, int maxItems)	throws DAOException {
		try {
			return getCollectionsChildren(repoDao, parentId, scope, filter, Arrays.asList(new String[]{"folders"}), sortDefinition, skipCount, maxItems);
		} catch (Exception e) {
			throw DAOException.mapping(e);
		}
	}
	private static CollectionBaseEntries getCollectionsChildren(RepositoryDao repoDao, String parentId, SearchScope scope, Filter propFilter, List<String> filter, SortDefinition sortDefinition, int skipCount, int maxItems) throws DAOException {
		List<CollectionBase> result = new ArrayList<CollectionBase>();
		// if this collection is ordered by user, use the position of the elements as primary order criteria
		if(!ROOT.equals(parentId) && CCConstants.COLLECTION_ORDER_MODE_CUSTOM.equals(getCollection(repoDao, parentId).getOrderMode())) {
			sortDefinition.addSortDefinitionEntry(
					new SortDefinition.SortDefinitionEntry(CCConstants.getValidLocalName(CCConstants.CCM_PROP_COLLECTION_ORDERED_POSITION),true),0);
		}

		List<org.alfresco.service.cmr.repository.NodeRef> children =
				repoDao.getCollectionClient().getChildren(
						ROOT.equals(parentId) ? null : parentId,
						scope!=null ? scope.toString() : null,sortDefinition,filter);


		//NodeDao.convertAlfrescoNodeRef(repoDao,children)
		NodeEntries sorted = NodeDao.convertToRest(repoDao,Filter.createShowAllFilter(),NodeDao.convertAlfrescoNodeRef(repoDao,children),skipCount,maxItems);
		Pagination pagination = sorted.getPagination();
		for (Node child : sorted.getNodes()) {

			String nodeType = child.getType();

			if (CCConstants.getValidLocalName(CCConstants.CCM_TYPE_MAP).equals(nodeType)) {

				// it's a collection

				//Collection collection = getCollection(repoDao, nodeId).asCollection();
				Collection collection = child.getCollection();

				result.add(collection);

			} else if (CCConstants.getValidLocalName(CCConstants.CCM_TYPE_IO).equals(nodeType)){

				// it's a references

				CollectionReference collRef = new CollectionReference();

				NodeRef ref=child.getRef();
				collRef.setRef(ref);

				String shortproporiginal = CCConstants.getValidLocalName(CCConstants.CCM_PROP_IO_ORIGINAL);
				if(!propFilter.getProperties().contains(shortproporiginal)){
					propFilter.getProperties().add(shortproporiginal);
				}

				final Node node=child;
				collRef.setReference(node);
				collRef.setAccess(node.getAccess());
				HashMap<String,String[]> props = collRef.getReference().getProperties();
				String[] prop=props.get(shortproporiginal);
				final String originalId=prop!=null && prop.length>0 ? prop[0] : null;
				collRef.setOriginalId(originalId);
				try{
					collRef.setAccessOriginal(NodeDao.getNode(repoDao,originalId).asNode().getAccess());
				}catch(Throwable t){
					// user may has no access to the original, this is okay
				}
				AuthenticationUtil.runAsSystem(new RunAsWork<Void>() {

					@Override
					public Void doWork() throws Exception {
						try{
							NodeDao nodeDaoOriginal = NodeDao.getNode(repoDao,originalId);
							node.setCreatedBy(nodeDaoOriginal.asNode().getCreatedBy());
						}catch(Throwable t){
							collRef.setOriginalId(null);
							// original maybe deleted
						}
						return null;
					}

				});

				Preview preview = collRef.getReference().getPreview();
				collRef.setPreview(preview);

				result.add(collRef);
			}
		}
		CollectionBaseEntries obj=new CollectionBaseEntries();
		obj.setEntries(result);
		obj.setPagination(pagination);
		return obj;
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

			this.collection = unmarshalling(repoDao.getId(), collectionClient.get(nodeDao.getStoreIdentifier(),nodeDao.getStoreProtocol(),collectionId));
			this.baseClient = repoDao.getBaseClient();
			this.access = nodeDao.asNode().getAccess();//baseClient.hasAllPermissions(collectionId, PERMISSIONS);
			this.preview= nodeDao.asNode().getPreview();

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
			this.collection = unmarshalling(repoDao.getId(), collectionClient.get(nodeDao.getStoreIdentifier(),nodeDao.getStoreProtocol(),collectionId));
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

	public static CollectionDao createRoot(RepositoryDao repoDao, Collection collection) throws DAOException {

		try {

			org.edu_sharing.service.collection.Collection child = 
					repoDao.getCollectionClient().createAndSetScope(null,marshalling(collection));
			return new CollectionDao(repoDao, child.getNodeId());

		} catch (Throwable t) {

			throw DAOException.mapping(t);
		}
			
	}

	public CollectionDao createChild(Collection collection) throws DAOException {

		try {

			org.edu_sharing.service.collection.Collection child = collectionClient.createAndSetScope(collectionId, marshalling(collection));
			
			return new CollectionDao(repoDao, child.getNodeId());

		} catch (Throwable t) {

			throw DAOException.mapping(t);
		}
			
	}

	public void update(Collection collection) throws DAOException {

		try {
		
			collectionClient.updateAndSetScope(marshalling(collection));
			
		} catch (Exception e) {

			throw DAOException.mapping(e);
		}
		
	}
	
	public void delete() throws DAOException {

		try {
		
			collectionClient.remove(collection.getRef().getId());
			
		} catch (Exception e) {

			throw DAOException.mapping(e);
		}
			
	}

	public void addToCollection(NodeDao node) throws DAOException {
		
		try {
		
			collectionClient.addToCollection(collection.getRef().getId(), node.getRef().getId());
			
		} catch (Throwable t) {

			throw DAOException.mapping(t);
		}
			
	}
	
	public void removeFromCollection(NodeDao node) throws DAOException {
		
		try {
		
			collectionClient.removeFromCollection(collection.getRef().getId(), node.getRef().getId());
			
		} catch (Exception e) {

			throw DAOException.mapping(e);
		}
			
	}

	public Collection asCollection() throws DAOException {

		this.collection.setAccess(getAccess());
		this.collection.setPreview(preview);
		return this.collection;
	}
	
	
	private List<String> getAccess() {
		return access;
	}

	
	private static org.edu_sharing.service.collection.Collection marshalling(Collection collection) {
		
		if (collection == null) {
			return null;
		}
		
		org.edu_sharing.service.collection.Collection result = new org.edu_sharing.service.collection.Collection();
		
		result.setColor(collection.getColor());
		result.setDescription(collection.getDescription());
		result.setLevel0(collection.isLevel0());
		
		if (collection.getRef() != null) {
			
			result.setNodeId(collection.getRef().getId());
		}
		
		result.setTitle(collection.getTitle());
		result.setType(collection.getType());
		result.setViewtype(collection.getViewtype());
		result.setX(collection.getX());
		result.setY(collection.getY());
		result.setZ(collection.getZ());
		result.setPinned(collection.isPinned());
		
		if (collection.getRef() != null) {
			
			result.setNodeId(collection.getRef().getId());
		}
		
		if (collection.getOwner() != null){
			
			org.edu_sharing.repository.client.rpc.User user = new org.edu_sharing.repository.client.rpc.User();
			user.setAuthorityName(collection.getOwner().getUserName());
			user.setEmail(collection.getOwner().getProfile().getEmail());
			user.setGivenName(collection.getOwner().getProfile().getFirstName());
			user.setSurname(collection.getOwner().getProfile().getLastName());
			user.setUsername(collection.getOwner().getUserName());
		}
		
		result.setScope(collection.getScope());
		
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
		
		if (collection.getOwner() != null) {
		
		User user = new User();
		user.setProfile(new UserProfile());
		user.getProfile().setEmail(collection.getOwner().getEmail());
		user.getProfile().setFirstName(collection.getOwner().getGivenName());
		user.getProfile().setLastName(collection.getOwner().getSurname());
		
		NodeRef userNodeRef = new NodeRef();
		userNodeRef.setId(collection.getOwner().getNodeId());
		userNodeRef.setRepo(repoId);
		//user.setRef(userNodeRef);
		user.setUserName(collection.getOwner().getUsername());

		result.setOwner(user);
		}
		
		result.setChildCollectionsCount(collection.getChildCollectionsCount());
		result.setChildReferencesCount(collection.getChildReferencesCount());
		
		if (collection.getNodeId() != null) {
			
			NodeRef ref = new NodeRef();
			ref.setRepo(repoId);
			ref.setId(collection.getNodeId());
			
			result.setRef(ref);
		}
		
		result.setTitle(collection.getTitle());
		result.setType(collection.getType());
		result.setOrderMode(collection.getOrderMode());
		result.setViewtype(collection.getViewtype());
		result.setX(collection.getX());
		result.setY(collection.getY());
		result.setZ(collection.getZ());
		result.setPinned(collection.isPinned());

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

	
}
