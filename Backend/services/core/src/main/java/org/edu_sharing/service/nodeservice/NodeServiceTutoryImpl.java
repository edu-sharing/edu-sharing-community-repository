package org.edu_sharing.service.nodeservice;

import java.util.Map;

import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.appcontext.ApplicationInfoContextHolder;
import org.edu_sharing.service.search.SearchServiceTutoryImpl;
import org.json.JSONObject;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Lazy
@Service
public class NodeServiceTutoryImpl extends NodeServiceAdapter {

	
	@Override
	public Map<String, Object> getProperties(String storeProtocol, String storeId, String nodeId) throws Throwable {
		
		String httpResult = SearchServiceTutoryImpl.getHttpResult("https://www.tutory.de/api/v1/worksheet/" + nodeId);
		
		JSONObject worksheet = new JSONObject(httpResult);
		Map<String, Object> properties = SearchServiceTutoryImpl.getProperties(worksheet);

        String appId = ApplicationInfoContextHolder.getCurrentApplicationInfo().getAppId();
		properties.put(CCConstants.REPOSITORY_ID, appId);
		
		return properties;
	}
	
	
	
}
