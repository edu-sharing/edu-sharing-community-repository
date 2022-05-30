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

public class SystemFolder {

    static Logger logger = Logger.getLogger(SystemFolder.class);

    public static NodeRef getSystemObject(String name) throws Throwable {
        String folder = ToolPermissionServiceFactory.getInstance().getEdu_SharingSystemFolderBase().getId();
        NodeService nodeService= NodeServiceFactory.getLocalService();
        String node=nodeService.findNodeByName(folder, name);
        if(node==null){
            HashMap<String, Object> props=new HashMap<>();
            props.put(CCConstants.CM_NAME,name);
            node=nodeService.createNodeBasic(folder,CCConstants.CCM_TYPE_IO,props);
        }
        return new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,node);
    }

    public static <T> T getSystemObjectContent(String name, Class<T> clazz){
        return AuthenticationUtil.runAsSystem(()-> {
            try {
                NodeRef node = getSystemObject(name);
                InputStream content = NodeServiceHelper.getContent(node);
                if (content == null) {
                    return clazz.newInstance();
                }
                return new Gson().fromJson(new InputStreamReader(content), clazz);
            } catch (Throwable t) {
                logger.warn(t.getMessage(),t);
                return clazz.newInstance();
            }
        });
    }
}
