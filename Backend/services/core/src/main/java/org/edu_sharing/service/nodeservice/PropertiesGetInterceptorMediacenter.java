package org.edu_sharing.service.nodeservice;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.apache.commons.lang.StringUtils;
import org.edu_sharing.repository.client.tools.CCConstants;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class PropertiesGetInterceptorMediacenter extends PropertiesGetInterceptorDefault {
    @Override
    public Map<String, Object> beforeCacheProperties(PropertiesContext context) {

        AuthenticationUtil.runAsSystem(()->{
            long aclId = serviceRegistry.getNodeService().getNodeAclId(context.getNodeRef());
            Set<String> readers = serviceRegistry.getPermissionService().getReaders(aclId);
            context.getProperties().put(CCConstants.VIRT_PROP_MEDIACENTERS, StringUtils.join(readers
                    .stream().filter(a -> a.startsWith("GROUP_MEDIA_CENTER_")).map(a -> serviceRegistry.getAuthorityService().getAuthorityDisplayName(a)).collect(Collectors.toList()),CCConstants.MULTIVALUE_SEPARATOR));
            return null;
        });

        return context.getProperties();
    }
}
