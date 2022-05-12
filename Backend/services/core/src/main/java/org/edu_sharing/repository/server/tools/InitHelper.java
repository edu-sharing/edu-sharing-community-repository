package org.edu_sharing.repository.server.tools;

import com.typesafe.config.Config;
import org.alfresco.service.cmr.security.PermissionService;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.authority.AuthorityService;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceImpl;

import java.util.List;

public class InitHelper {
    static Logger logger = Logger.getLogger(InitHelper.class);
    public static void initGroups() throws Exception {
        List<? extends Config> createGroups = LightbendConfigLoader.get().getConfigList("repository.groups.create");
        if(createGroups != null && !createGroups.isEmpty()) {
            AuthorityService authorityService = AuthorityServiceFactory.getLocalService();
            for(Config group: createGroups) {
                String id = group.getString("id");
                if(!id.startsWith(PermissionService.GROUP_PREFIX)) {
                    id = PermissionService.GROUP_PREFIX + id;
                }
                if(authorityService.getAuthorityNodeRef(id) == null) {
                    logger.info("Init group " + id);
                    authorityService.createGroup(id, group.getString("displayName"), null);
                }
            }
        }
    }
}
