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
        String folder = ToolPermissionServiceFactory.getInstance().getEdu_SharingSystemFolderBase().getId();
        NodeService nodeService= NodeServiceFactory.getLocalService();
        String node=nodeService.findNodeByName(folder, CCConstants.CCM_VALUE_IO_NAME_CONFIG_NODE_NAME);
        if(node==null){
            HashMap<String, Object> props=new HashMap<>();
            props.put(CCConstants.CM_NAME,CCConstants.CCM_VALUE_IO_NAME_CONFIG_NODE_NAME);
            node=nodeService.createNodeBasic(folder,CCConstants.CCM_TYPE_IO,props);
        }
        return new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,node);
    }
    public static RepositoryConfig getConfig(){
        return AuthenticationUtil.runAsSystem(()-> {
            try {
                NodeRef node = getConfigNode();
                InputStream content = NodeServiceHelper.getContent(node);
                if (content == null) {
                    // init an empty config
                    return new RepositoryConfig();
                }
                return new Gson().fromJson(new InputStreamReader(content), RepositoryConfig.class);
            } catch (Throwable t) {
                logger.warn(t.getMessage(),t);
                return new RepositoryConfig();
            }
        });
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
