package org.edu_sharing.repository.server;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.edu_sharing.repository.client.CollectionService;
import org.edu_sharing.repository.client.exception.CCException;
import org.edu_sharing.repository.client.rpc.Collection;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.restservices.RepositoryDao;
import org.edu_sharing.service.collection.CollectionServiceFactory;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

public class CollectionServiceImpl extends RemoteServiceServlet implements CollectionService {
	
	
	@Override
	public String addToCollection(String collectionId, String originalNodeId) throws CCException{
		try{
			org.edu_sharing.service.collection.CollectionService collectionService = CollectionServiceFactory.getCollectionService(ApplicationInfoList.getHomeRepository().getAppId());
			return collectionService.addToCollection(collectionId, originalNodeId);
		}catch(Throwable e){
			errorHandling(e);
			return null;
		}
	}
	
	@Override
	public String[] addToCollection(String collectionId, String[] originalNodeIds) throws CCException{
		try{
			org.edu_sharing.service.collection.CollectionService collectionService = CollectionServiceFactory.getCollectionService(ApplicationInfoList.getHomeRepository().getAppId());
			return collectionService.addToCollection(collectionId, originalNodeIds);
		}catch(Throwable e){
			errorHandling(e);
			return null;
		}
	}
	
	@Override
	public Collection create(String collectionId, Collection collection) throws Throwable {
		org.edu_sharing.service.collection.CollectionService collectionService = CollectionServiceFactory.getCollectionService(ApplicationInfoList.getHomeRepository().getAppId());
		return marshallToClientObject(collectionService.create(collectionId, marshallToServiceObject(collection)));
	}
	
	@Override
	public Collection get(String storeId,String storeProtocol,String collectionId) {
		org.edu_sharing.service.collection.CollectionService collectionService = CollectionServiceFactory.getCollectionService(ApplicationInfoList.getHomeRepository().getAppId());
		return marshallToClientObject(collectionService.get(storeId,storeProtocol,collectionId));
	}
	
	@Override
	public HashMap<String, HashMap<String, Object>> getChildren(String parentId, String scope) {
		org.edu_sharing.service.collection.CollectionService collectionService = CollectionServiceFactory.getCollectionService(ApplicationInfoList.getHomeRepository().getAppId());
		return collectionService.getChildren(parentId, scope);
	}
	
	@Override
	public void move(String toCollection, String nodeId) {
		org.edu_sharing.service.collection.CollectionService collectionService = CollectionServiceFactory.getCollectionService(ApplicationInfoList.getHomeRepository().getAppId());
		collectionService.move(toCollection, nodeId);
	}
	
	@Override
	public void remove(String collectionId) {
		org.edu_sharing.service.collection.CollectionService collectionService = CollectionServiceFactory.getCollectionService(ApplicationInfoList.getHomeRepository().getAppId());
		collectionService.remove(collectionId);
	}
	
	@Override
	public void removeFromCollection(String collectionId, String nodeId) {
		org.edu_sharing.service.collection.CollectionService collectionService = CollectionServiceFactory.getCollectionService(ApplicationInfoList.getHomeRepository().getAppId());
		collectionService.removeFromCollection(collectionId,nodeId);
	}
	
	@Override
	public void update(Collection collection) {
		org.edu_sharing.service.collection.CollectionService collectionService = CollectionServiceFactory.getCollectionService(ApplicationInfoList.getHomeRepository().getAppId());
		collectionService.update(marshallToServiceObject(collection));
		
	}

	
	Collection marshallToClientObject(org.edu_sharing.service.collection.Collection collection){
		Collection col = new Collection();
		col.setColor(collection.getColor());
		col.setDescription(collection.getDescription());
		col.setLevel0(collection.isLevel0());
		col.setNodeId(collection.getNodeId());
		col.setTitle(collection.getTitle());
		col.setType(collection.getType());
		col.setViewtype(collection.getViewtype());
		col.setX(collection.getX());
		col.setY(collection.getY());
		col.setZ(collection.getZ());
		return col;
	}
	
	org.edu_sharing.service.collection.Collection marshallToServiceObject(Collection collection){
		org.edu_sharing.service.collection.Collection col = new org.edu_sharing.service.collection.Collection();
		col.setColor(collection.getColor());
		col.setDescription(collection.getDescription());
		col.setLevel0(collection.isLevel0());
		col.setNodeId(collection.getNodeId());
		col.setTitle(collection.getTitle());
		col.setType(collection.getType());
		col.setViewtype(collection.getViewtype());
		col.setX(collection.getX());
		col.setY(collection.getY());
		col.setZ(collection.getZ());
		return col;
	}
	
	private void errorHandling(Throwable e) throws CCException {
		throw new CCException(CCException.UNKNOWNEXCEPTION, e.getMessage());
	}
	
}
