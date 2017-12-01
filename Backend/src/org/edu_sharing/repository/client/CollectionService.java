package org.edu_sharing.repository.client;

import java.util.HashMap;

import org.edu_sharing.repository.client.exception.CCException;
import org.edu_sharing.repository.client.rpc.Collection;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("eduservlet/collectionservice")
public interface CollectionService extends RemoteService {
	
	
	public static final String SCOPE_MY = "SCOPE_MY";
	public static final String SCOPE_EDU_GROUPS = "SCOPE_EDU_GROUPS";
	public static final String SCOPE_EDU_ALL = "SCOPE_ALL";
	
	public Collection create(String collectionId, Collection collection) throws Throwable;
	
	public void update(Collection collection);
	
	public void remove(String collectionId);
	
	
	/**
	 * /**
	 * creates an refObject
	 * adds Usage to Original IO (lms=repo,course=sammlungsid,resourceId=refIoId, nodeId=originalIOId
	 * 
	 * 
	 * @param collectionId
	 * @param originalNodeId
	 * @return nodeId of the refObject
	 */
	public String addToCollection(String collectionId, String originalNodeId) throws CCException;
	
	
	
	/**
	 * removes refobject or collection
	 * removesUsage
	 * 
	 * @param collectionId
	 * @param nodeId
	 */
	public void removeFromCollection(String collectionId, String nodeId);
	
	public void move(String toCollection, String nodeId);
	
	public Collection get(String storeId, String storeProtocol, String collectionId);


	public HashMap<String,HashMap<String,Object>> getChildren(String parentId, String scope);

	String[] addToCollection(String collectionId, String[] originalNodeIds) throws CCException;


}
