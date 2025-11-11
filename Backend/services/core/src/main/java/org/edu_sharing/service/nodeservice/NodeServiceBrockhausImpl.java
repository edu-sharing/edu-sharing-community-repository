package org.edu_sharing.service.nodeservice;

import org.apache.commons.lang3.StringUtils;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.search.SearchServiceBrockhausImpl;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Lazy
@Service
public class NodeServiceBrockhausImpl extends NodeServiceAdapterCached {


	@Override
	public Map<String, Object> getProperties(String storeProtocol, String storeId, String nodeId) throws Throwable {
		Map<String, Object> props = super.getProperties(storeProtocol, storeId, nodeId);
		if (props == null) {
			props = new HashMap<>();
		}
		String url = SearchServiceBrockhausImpl.buildUrl(nodeId);
		String name = StringUtils.substringAfterLast(nodeId, "%2f");
		props.put(CCConstants.CM_NAME,name);
		props.put(CCConstants.CONTENTURL,url);
		props.put(CCConstants.CCM_PROP_IO_WWWURL,url);

		return props;
	}
	@Override
	public Map<String, Object> getPropertiesDynamic(String storeProtocol, String storeId, String nodeId) throws Throwable {
		return getProperties(storeProtocol, storeId, nodeId);
	}

	@Override
	public InputStream getContent(String nodeId) throws Throwable{
		return null;
	}
}
