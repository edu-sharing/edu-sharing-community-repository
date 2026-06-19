package org.edu_sharing.service.nodeservice;

import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.appcontext.ApplicationInfoContextHolder;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.service.search.SearchServiceOersiImpl;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Lazy
@Service
public class NodeServiceOersiImpl extends NodeServiceAdapter {

    private static final Logger logger = Logger.getLogger(NodeServiceOersiImpl.class);

    @Override
    public Map<String, Object> getProperties(String storeProtocol, String storeId, String nodeId) throws Throwable {
        Map<String, Object> properties = new SearchServiceOersiImpl().retrieveNode(nodeId);
        ApplicationInfo appInfo = ApplicationInfoContextHolder.getCurrentApplicationInfo();
        properties.put(CCConstants.REPOSITORY_ID, appInfo.getAppId());
        return new HashMap<>(properties);
    }

    @Override
    public String getType(String storeProtocol, String storeId, String nodeId) {
        return CCConstants.CCM_TYPE_IO;
    }

}
