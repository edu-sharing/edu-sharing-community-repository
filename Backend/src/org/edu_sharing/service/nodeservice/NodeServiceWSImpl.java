package org.edu_sharing.service.nodeservice;

import java.io.InputStream;
import java.net.URLEncoder;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.List;

import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.UrlTool;
import org.edu_sharing.repository.server.authentication.Context;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.webservices.alfresco.extension.KeyValue;
import org.edu_sharing.webservices.alfresco.extension.NativeAlfrescoWrapper;
import org.edu_sharing.webservices.alfresco.extension.RepositoryNode;
import org.edu_sharing.webservices.util.EduWebServiceFactory;

public class NodeServiceWSImpl extends NodeServiceAdapter {
	
	ApplicationInfo appInfo;
	
	Logger logger = Logger.getLogger(NodeServiceWSImpl.class);
	
	public NodeServiceWSImpl(String appId) {
		super(appId);
		appInfo = ApplicationInfoList.getRepositoryInfoById(appId);
	}

	@Override
	public void updateNode(String nodeId, HashMap<String, String[]> props) throws Throwable {
	}

	@Override
	public String createNode(String parentId, String nodeType, HashMap<String, String[]> props) throws Throwable {
		return null;
	}

	@Override
	public String createNodeBasic(String parentID, String nodeTypeString, HashMap<String, Object> _props) {
		return null;
	}

	@Override
	public String findNodeByName(String parentId, String name) {
		return null;
	}

	@Override
	public NodeRef copyNode(String sourceId, String nodeId, boolean withChildren) throws Throwable {
		return null;
	}

	@Override
	public String getCompanyHome() {
		return null;
	}

	@Override
	public HashMap<String, String[]> getNameProperty(String name) {
		return null;
	}

	@Override
	public HashMap<String, Object> getChild(StoreRef store, String parentId, String type, String property,
			String value) {
		return null;
	}

	@Override
	public void setOwner(String nodeId, String username) {
	}

	@Override
	public void setPermissions(String nodeId, String authority, String[] permissions, Boolean inheritPermission)
			throws Exception {
	}

	@Override
	public String getOrCreateUserInbox() {
		return null;
	}
	
	@Override
	public List<ChildAssociationRef> getChildrenChildAssociationRef(String parentID) {
		return null;
	}
	
	@Override
	public void createVersion(String nodeId, HashMap _properties) throws Exception {
	}
	
	@Override
	public void writeContent(StoreRef store, String nodeID, InputStream content, String mimetype, String _encoding,
			String property) throws Exception {
	}
	
	@Override
	public void removeNode(String nodeID, String fromID) {
	}
	
	@Override
	public HashMap<String, Object> getProperties(String storeProtocol, String storeId, String nodeId) throws Throwable {
		try{
			NativeAlfrescoWrapper stub = EduWebServiceFactory.getNativeAlfrescoWrapper(appInfo.getWebServiceHotUrl());
			return stub.getPropertiesExt(storeProtocol, storeId, nodeId);
		}catch(RemoteException e){
			logger.error(e.getMessage(), e);
			return null;
		}
	}
	
	@Override
	public String[] getAspects(String storeProtocol, String storeId, String nodeId)  {
		try{
			NativeAlfrescoWrapper stub = EduWebServiceFactory.getNativeAlfrescoWrapper(appInfo.getWebServiceHotUrl());
			return stub.getAspects(storeProtocol, storeId, nodeId);
		}catch(RemoteException e){
			logger.error(e.getMessage(), e);
			return null;
		}
	}
	
	@Override
	public void addAspect(String nodeId, String aspect) {
		try{
			NativeAlfrescoWrapper stub = EduWebServiceFactory.getNativeAlfrescoWrapper(appInfo.getWebServiceHotUrl());
			stub.addAspect(nodeId, aspect);
		}catch(RemoteException e){
			logger.error(e.getMessage(), e);
		}
	}
	
	@Override
	public void moveNode(String newParentId, String childAssocType, String nodeId) {
		try{
			NativeAlfrescoWrapper stub = EduWebServiceFactory.getNativeAlfrescoWrapper(appInfo.getWebServiceHotUrl());
			stub.moveNode(newParentId, childAssocType, nodeId);
		}catch(RemoteException e){
			logger.error(e.getMessage(), e);
		}
	}
	
	@Override
	public void revertVersion(String nodeId, String verLbl) throws Exception {
		try{
			NativeAlfrescoWrapper stub = EduWebServiceFactory.getNativeAlfrescoWrapper(appInfo.getWebServiceHotUrl());
			stub.revertVersion(nodeId, verLbl);
		}catch(RemoteException e){
			logger.error(e.getMessage(), e);
		}
	}
	
	@Override
	public HashMap<String, HashMap<String, Object>> getVersionHistory(String nodeId) throws Exception {
		try{
			NativeAlfrescoWrapper stub = EduWebServiceFactory.getNativeAlfrescoWrapper(appInfo.getWebServiceHotUrl());
			RepositoryNode[] repoNodes = stub.getVersionHistory(nodeId);
			if(repoNodes != null && repoNodes.length > 0){
				
				HashMap<String, HashMap<String,Object>>  result = new HashMap<String, HashMap<String,Object>> ();
				for(RepositoryNode repNode:repoNodes){
					HashMap<String,Object> properties = new HashMap<String,Object>();
					for(KeyValue property : repNode.getProperties()){
						properties.put(property.getKey(), property.getValue());
					}
					
					//overwrite content and download URL
					String contentUrl = this.getRedirectServletLink(appInfo.getAppId(), (String) properties.get(CCConstants.SYS_PROP_NODE_UID));
					properties.put(CCConstants.CONTENTURL, contentUrl);
					if(contentUrl != null){
						String params = URLEncoder.encode("display=download");
						String downLoadUrl = UrlTool.setParam(contentUrl, "params", params);
						properties.put(CCConstants.DOWNLOADURL, downLoadUrl);
					}
					
					result.put(repNode.getNodeId(), properties);
				}
				return result;
			}
			
		} catch(RemoteException e) {
			logger.error(e.getMessage(), e);
		}
		return null;
	}
	
	protected String getRedirectServletLink(String repId, String nodeId){
		
		ApplicationInfo homeRepository = ApplicationInfoList.getHomeRepository();
		
		String hostOrDomain = (homeRepository.getDomain() == null || homeRepository.getDomain().trim().equals(""))? homeRepository.getHost() : homeRepository.getDomain();
		
		String url = homeRepository.getClientprotocol()+"://"+hostOrDomain+":"+homeRepository.getClientport()+"/"+homeRepository.getWebappname() + "/" + CCConstants.EDU_SHARING_SERVLET_PATH_REDIRECT;
		
		//if no cookies are allowed render jsessionid in url. Attention: the host or domain in appinfo must match the client ones
		Context context = Context.getCurrentInstance();
		//context can be null when not accessing true ContextManagementFilter (i.i by calling nativealfrsco webservice)
		if (context != null) {
			url = context.getResponse().encodeURL(url);
		}
		
		url = UrlTool.setParam(url, "APP_ID",  repId);
		url =  UrlTool.setParam(url,"NODE_ID", nodeId);
		return url;
	}

	@Override
	public String importNode(String nodeId, String localParent) throws Throwable {
		return null;
	}

}
