package org.edu_sharing.service.collection;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.edu_sharing.restservices.shared.Filter;
import org.edu_sharing.service.search.model.SortDefinition;

public interface CollectionService {
	

	
	public Collection create(String collectionId, Collection collection) throws Throwable;
	
	public void update(Collection collection);
	
	public void remove(String collectionId);
	
	
	/**
	 * /**
	 * creates an refObject
	 * adds Usage to Original IO (lms=repo,course=sammlungsid,resourceId=refIoId, nodeId=originalIOId
	 * @param repoDao 
	 * 
	 * 
	 * @param collectionId
	 * @param originalNodeId
	 * @return nodeId of the refObject
	 * @throws DuplicateNodeException 
	 * @throws Throwable 
	 */
	public String addToCollection(String collectionId, String originalNodeId) throws DuplicateNodeException, Throwable;
	
	
	public String[] addToCollection(String collectionId, String[] originalNodeIds) throws DuplicateNodeException, Throwable;
	
	
	/**
	 * removes refobject or collection
	 * removesUsage
	 * 
	 * @param collectionId
	 * @param nodeId
	 */
	public void removeFromCollection(String collectionId, String nodeId);
	
	public void move(String toCollection, String nodeId);
	
	public HashMap<String,HashMap<String,Object>> getChildren(String parentId, String scope);
	
	public List<NodeRef> getChildren(String parentId, String scope, SortDefinition sortDefinition,List<String> filter);

	public Collection get(String storeId, String storeProtocol, String collectionId);
	
	/**
	 * 
	 * @param parentId collection to set the order of the children
	 * @param nodes Children in the order. Use null to reset the order
	 */
	public void setOrder(String parentId, String[] nodes);

	void updateAndSetScope(Collection collection) throws Exception;

	Collection createAndSetScope(String parentId, Collection collection) throws Throwable;

    public void setPinned(String[] collections);

	void writePreviewImage(String collectionId, InputStream is, String mimeType) throws Exception;

    List<org.edu_sharing.service.model.NodeRef> getReferenceObjects(String nodeId);
}
