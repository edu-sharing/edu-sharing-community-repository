package org.edu_sharing.service.nodeservice;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.collections.map.LRUMap;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.search.SearchServicePixabayImpl;

import com.google.common.collect.MapMaker;

public class NodeServicePixabayImpl extends NodeServiceAdapterCached{

	private String repositoryId;
	private String APIKey;
	private Logger logger= Logger.getLogger(NodeServicePixabayImpl.class);

	public NodeServicePixabayImpl(String appId) {
		super(appId);
		ApplicationInfo appInfo = ApplicationInfoList.getRepositoryInfoById(appId);
		this.repositoryId = appInfo.getAppId();		
		APIKey = appInfo.getApiKey(); 
	}
	
	@Override
	public InputStream getContent(String nodeId) throws Throwable {
		try {
			HashMap<String, Object> properties = getProperties(null, null, nodeId);
			String download = (String) properties.get(CCConstants.CCM_PROP_IO_THUMBNAILURL);
			URL url = new URL(download);
			HttpsURLConnection connection = SearchServicePixabayImpl.openPixabayUrl(url);
			connection.connect();
			return connection.getInputStream();
		}catch(Throwable t){
			// this is likely to fail since pixabay does block any programatic image access
			logger.warn("Can not fetch inputStream from pixabay for node "+nodeId+": "+t.getMessage(),t);
			return null;
		}
	}
	@Override
	public HashMap<String, Object> getProperties(String storeProtocol, String storeId, String nodeId) throws Throwable {
		HashMap<String, Object> props = super.getProperties(storeProtocol, storeId, nodeId);
		if(props != null) {
			return props;
		}
		
		// Querying by "id" is no longer supported.
		// some api keys still have it, we can still try it
		
		try{
			SearchResultNodeRef list = SearchServicePixabayImpl.searchPixabay(repositoryId, APIKey, "&id="+nodeId);
			if(list.getData()!=null && list.getData().size()>0){
				updateCache(list.getData().get(0).getProperties());
				return list.getData().get(0).getProperties();
			}
		}
		catch(Throwable t){
			t.printStackTrace();
		}
		
		throw new Exception("Node "+nodeId+" was not found (cache expired)");
	}
}
