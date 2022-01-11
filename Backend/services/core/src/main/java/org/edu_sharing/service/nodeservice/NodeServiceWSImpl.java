package org.edu_sharing.service.nodeservice;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URLEncoder;
import java.rmi.RemoteException;
import java.util.*;
import java.util.stream.Collectors;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.MLText;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.UrlTool;
import org.edu_sharing.repository.client.tools.metadata.ValueTool;
import org.edu_sharing.alfresco.repository.server.authentication.Context;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import io.swagger.client.model.NodeEntry;
import org.edu_sharing.restservices.shared.Filter;
import org.edu_sharing.service.remote.RemoteObjectService;
import org.edu_sharing.service.repoproxy.RepoProxyFactory;
import org.edu_sharing.webservices.alfresco.extension.KeyValue;
import org.edu_sharing.webservices.alfresco.extension.NativeAlfrescoWrapper;
import org.edu_sharing.webservices.alfresco.extension.RepositoryNode;
import org.edu_sharing.webservices.util.EduWebServiceFactory;
import org.springframework.context.ApplicationContext;

public class NodeServiceWSImpl extends NodeServiceAdapter {

	private final ServiceRegistry serviceRegistry;
	ApplicationInfo appInfo;
	
	Logger logger = Logger.getLogger(NodeServiceWSImpl.class);
	
	public NodeServiceWSImpl(String appId) {
		super(appId);
		appInfo = ApplicationInfoList.getRepositoryInfoById(appId);
		ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
		serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
	}

	@Override
	public void updateNode(String nodeId, HashMap<String, String[]> props) throws Throwable {
	}

	@Override
	public String createNode(String parentId, String nodeType, HashMap<String, String[]> props) throws Throwable {
		return null;
	}

	@Override
	public String createNodeBasic(String parentID, String nodeTypeString, HashMap<String, ?> _props) {
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
	public void createVersion(String nodeId) throws Exception {
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
		NodeEntry entry = (NodeEntry) RepoProxyFactory.getRepoProxy().getMetadata(appId, nodeId, Collections.singletonList(Filter.ALL), null).getEntity();
		HashMap<String, Object> result = new HashMap<>();
		for(Map.Entry<String, List<String>> e : entry.getNode().getProperties().entrySet()){
			String globalKey = CCConstants.getValidGlobalName(e.getKey());
			if(e.getKey().startsWith("virtual:")) {
				result.put(globalKey, ValueTool.toMultivalue(e.getValue().toArray(new String[0])));
			} else if (e.getValue() != null && globalKey != null) {
				result.put(globalKey, e.getValue());
				if (e.getValue().size() == 1) {
					result.put(globalKey, e.getValue().get(0));
				} else {
					result.put(globalKey, e.getValue());
				}
				/*PropertyDefinition definition = null;
				try {
					definition = serviceRegistry.getDictionaryService().getProperty(QName.createQName(globalKey));
				}catch (Throwable ignored){ }
				if(definition != null) {
					PropertyDefinition finalDefinition = definition;
					List<Object> data = e.getValue().stream().map((v) ->
							{
								try {
									if (finalDefinition.getDataType().getJavaClassName().equals(MLText.class.getName())) {
										return v;
									}
									return Class.forName(finalDefinition.getDataType().getJavaClassName()).getConstructor(String.class).newInstance(v);
								} catch (Throwable t) {
									logger.warn(t);
									return v;
								}
							}
					).collect(Collectors.toList());
					if (data.size() == 1) {
						result.put(globalKey, data.get(0));
					} else {
						result.put(globalKey, data);
					}
				}*/
			}
		}
		result.put(CCConstants.CCM_PROP_IO_THUMBNAILURL, entry.getNode().getPreview().getUrl());
		return result;
	}

	@Override
	public HashMap<String, Object> getPropertiesPersisting(String storeProtocol, String storeId, String nodeId) throws Throwable {
		HashMap<String, Object> props = getProperties(storeProtocol, storeId, nodeId);
		HashMap<String, Object> result = new HashMap<>();
		props.forEach((key, value) -> {
			if (Arrays.asList(
					CCConstants.CM_NAME,
					CCConstants.CM_PROP_TITLE,
					CCConstants.LOM_PROP_GENERAL_KEYWORD,
					CCConstants.LOM_PROP_GENERAL_DESCRIPTION
			).contains(key)) {
				result.put(key, value);
			}
		});
		return result;
	}



	@Override
	public HashMap<String, Object> getPropertiesDynamic(String storeProtocol, String storeId, String nodeId) throws Throwable {
		return RemoteObjectService.cleanupRemoteProperties(getProperties(storeProtocol, storeId, nodeId));
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
