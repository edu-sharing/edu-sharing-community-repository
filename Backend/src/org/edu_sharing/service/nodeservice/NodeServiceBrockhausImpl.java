package org.edu_sharing.service.nodeservice;

import org.apache.commons.collections.map.LRUMap;
import org.apache.log4j.Logger;
import org.apache.solr.common.util.Hash;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.search.SearchServiceBrockhausImpl;
import org.edu_sharing.service.search.SearchServicePixabayImpl;

import javax.net.ssl.HttpsURLConnection;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

public class NodeServiceBrockhausImpl extends NodeServiceAdapter{

	private String repositoryId;
	private Logger logger= Logger.getLogger(NodeServiceBrockhausImpl.class);
	private String apiKey;

	public NodeServiceBrockhausImpl(String appId) {
		super(appId);
		ApplicationInfo appInfo = ApplicationInfoList.getRepositoryInfoById(appId);
		this.repositoryId = appInfo.getAppId();		
		this.apiKey = appInfo.getApiKey();
	}

	@Override
	public HashMap<String, Object> getProperties(String storeProtocol, String storeId, String nodeId) throws Throwable {
		//throw new Exception("Node "+nodeId+" was not found (api call not supported)");
		HashMap<String, Object> properties = new HashMap<>();
		String url=SearchServiceBrockhausImpl.buildUrl(apiKey,nodeId);
		properties.put(CCConstants.LOM_PROP_TECHNICAL_LOCATION,url);
		return properties;
	}
	

}
