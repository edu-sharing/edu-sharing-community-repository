package org.edu_sharing.service.nodeservice;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;

import org.apache.camel.util.LRUCache;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.search.SearchServicePixabayImpl;

import com.google.common.collect.MapMaker;

public class NodeServicePixabayImpl extends NodeServiceAdapter{

	private String repositoryId;
	private String APIKey;
	private static ConcurrentMap<String,HashMap<String, Object>> propertyCache=new MapMaker().expiration(60, TimeUnit.MINUTES).makeMap();
	public NodeServicePixabayImpl(String appId) {
		super(appId);
		ApplicationInfo appInfo = ApplicationInfoList.getRepositoryInfoById(appId);
		this.repositoryId = appInfo.getAppId();		
		APIKey = appInfo.getApiKey(); 
	}
	
	@Override
	public InputStream getContent(String nodeId) throws Throwable {
		HashMap<String, Object> properties = getProperties(null, null, nodeId);
		String download=(String) properties.get(CCConstants.DOWNLOADURL);
		URL url=new URL(download);
		HttpsURLConnection connection = SearchServicePixabayImpl.openPixabayUrl(url);
		connection.connect();
		return connection.getInputStream();
	}
	public static void updateCache(String id,HashMap<String,Object> properties) {
		propertyCache.put(id, properties);
	}
	@Override
	public HashMap<String, Object> getProperties(String storeProtocol, String storeId, String nodeId) throws Throwable {
		if(propertyCache.containsKey(nodeId))
			return propertyCache.get(nodeId);
		
		// Querying by "id" is no longer supported.
		// some api keys still have it, we can still try it
		
		try{
			SearchResultNodeRef list = SearchServicePixabayImpl.searchPixabay(repositoryId, APIKey, "&id="+nodeId);
			if(list.getData()!=null && list.getData().size()>0){
				propertyCache.put(nodeId, list.getData().get(0).getProperties());
				return list.getData().get(0).getProperties();
			}
		}
		catch(Throwable t){
			t.printStackTrace();
		}
		
		throw new Exception("Node "+nodeId+" was not found (cache expired)");
	}

	@Override
	public InputStream getContent(String storeProtocol, String storeId, String nodeId, String version, String contentProp)
			throws Throwable {
		HashMap<String, Object> props = getProperties(storeProtocol, storeId, nodeId);
		HttpURLConnection url=SearchServicePixabayImpl.openPixabayUrl(new URL((String) props.get(CCConstants.DOWNLOADURL)));
		return url.getInputStream();
	}
	

}
