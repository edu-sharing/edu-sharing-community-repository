package org.edu_sharing.service.nodeservice;

import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.search.SearchServiceOersiImpl;

import java.util.HashMap;
import java.util.Map;

public class NodeServiceOersiImpl extends NodeServiceAdapter {

    private static final Logger logger = Logger.getLogger(NodeServiceOersiImpl.class);

    public NodeServiceOersiImpl(String appId) {
        super(appId);
    }

    @Override
    public Map<String, Object> getProperties(String storeProtocol, String storeId, String nodeId) throws Throwable {
        Map<String, Object> properties = new SearchServiceOersiImpl(appId).retrieveNode(nodeId);
        properties.put(CCConstants.REPOSITORY_ID, this.appId);
        return new HashMap<>(properties);
    }

    @Override
    public String getType(String storeProtocol, String storeId, String nodeId) {
        return CCConstants.CCM_TYPE_IO;
    }

}
