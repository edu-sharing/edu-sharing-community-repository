package org.edu_sharing.service.admin;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.admin.model.RepositoryConfig;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.service.config.ConfigServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.toolpermission.ToolPermissionServiceFactory;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;

@Slf4j
public class RepositoryConfigFactory {
    private static NodeRef getConfigNode() throws Throwable {
        return SystemFolder.getSystemObject(CCConstants.CCM_VALUE_IO_NAME_CONFIG_NODE_NAME);
    }

    public static RepositoryConfig.RepositoryMessage getSystemMessage() {
        List<RepositoryConfig.RepositoryMessage> msg = getConfig().getMessages();
        long now = System.currentTimeMillis();
        boolean isGuest = AuthorityServiceFactory.getLocalService().isGuest();

        if (!CollectionUtils.isEmpty(msg)) {
            return msg.stream()
                    .filter(m ->
                            userModeMatches(m, isGuest) &&
                                    (m.getFrom() == null || m.getFrom() <= now) &&
                                    (m.getTo() == null || m.getTo() >= now) &&
                                    (CollectionUtils.isEmpty(m.getContexts()) || m.getContexts().contains(ConfigServiceFactory.getCurrentContextId())) &&
                                    (CollectionUtils.isEmpty(m.getToolpermissions()) || m.getToolpermissions().stream().allMatch(tp -> ToolPermissionServiceFactory.getInstance().hasToolPermission(tp)))
                    )
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    private static boolean userModeMatches(RepositoryConfig.RepositoryMessage m, boolean isGuest) {
        var mode = m.getUserMode();
        return RepositoryConfig.RepositoryMessage.UserMode.all.equals(mode)
                || (RepositoryConfig.RepositoryMessage.UserMode.guest.equals(mode) && isGuest)
                || (RepositoryConfig.RepositoryMessage.UserMode.user.equals(mode) && !isGuest);
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
            log.warn(t.getMessage(),t);
            throw new RuntimeException(t);
        }
    }
}
