package org.edu_sharing.service.admin;

import com.google.gson.Gson;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.admin.model.RepositoryConfig;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.toolpermission.ToolPermissionServiceFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

public class RepositoryConfigFactory {
    private static Logger logger=Logger.getLogger(RepositoryConfigFactory.class);

    private static NodeRef getConfigNode() throws Throwable {
        return SystemFolder.getSystemObject(CCConstants.CCM_VALUE_IO_NAME_CONFIG_NODE_NAME);
    }
    public static RepositoryConfig getConfig(){
        return SystemFolder.getSystemObjectContent(CCConstants.CCM_VALUE_IO_NAME_CONFIG_NODE_NAME, RepositoryConfig.class);
    }
    public static void setConfig(RepositoryConfig config){
        try {
            NodeRef node = getConfigNode();
            if(config == null){
                NodeServiceFactory.getLocalService().removeNode(node.getId(), null);
                return;
            }
            String json = new Gson().toJson(config);
            NodeServiceHelper.writeContentText(node,json);
        } catch (Throwable t) {
            logger.warn(t.getMessage(),t);
        }
    }
}
