package org.edu_sharing.service.nodeservice;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.search.SearchServiceBrockhausImpl;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class NodeServiceBrockhausImpl extends NodeServiceAdapterCached{

	private ApplicationInfo appInfo;
	private String repositoryId;
	private Logger logger= Logger.getLogger(NodeServiceBrockhausImpl.class);

	public NodeServiceBrockhausImpl(String appId) {
		super(appId);
		if(appId != null) {
			this.appInfo = ApplicationInfoList.getRepositoryInfoById(appId);
			this.repositoryId = appInfo.getAppId();
		}
	}

	@Override
	public Map<String, Object> getPropertiesPersisting(String storeProtocol, String storeId, String nodeId) throws Throwable {
		Map<String, Object> props = super.getPropertiesPersisting(storeProtocol, storeId, nodeId);
		// provide the decoded id so the internal id of this document is stored and looked up properly in the RemoteObj folder
		props.put(CCConstants.SYS_PROP_NODE_UID, decodeId(nodeId));
		return props;
	}

	@Override
	public Map<String, Object> getProperties(String storeProtocol, String storeId, String nodeId) throws Throwable {
		nodeId = decodeId(nodeId);
		Map<String, Object> props = super.getProperties(storeProtocol, storeId, nodeId);
		if (props == null) {
			props = new HashMap<>();
		}
		String url = SearchServiceBrockhausImpl.buildUrl(appInfo, nodeId);
		String name = StringUtils.substringAfterLast(nodeId, "%2f");
		props.put(CCConstants.CM_NAME,name);
		props.put(CCConstants.CONTENTURL,url);
		props.put(CCConstants.CCM_PROP_IO_WWWURL,url);

		return props;
	}

	public static String encodeId(String nodeId) {
		if(nodeId.contains("%2f")) {
			return Base64.encodeBase64String(nodeId.getBytes(StandardCharsets.UTF_8));
		}
		return nodeId;
	}

	static String decodeId(String nodeId) {
		if(!nodeId.contains("%2f")) {
            return new String(Base64.decodeBase64(nodeId), StandardCharsets.UTF_8);
        }
		return nodeId;
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
