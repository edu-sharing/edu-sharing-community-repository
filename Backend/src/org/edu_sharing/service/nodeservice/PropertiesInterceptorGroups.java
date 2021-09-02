package org.edu_sharing.service.nodeservice;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.PermissionService;
import org.apache.commons.lang.StringUtils;
import org.edu_sharing.repository.client.tools.CCConstants;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class PropertiesInterceptorGroups extends PropertiesInterceptorImpl{

    @Override
    public Map<String, Object> beforeDeliverProperties(PropertiesContext context) {
        if(!context.getSource().equals(PropertiesCallSource.Search)){
            if(serviceRegistry.getPermissionService().hasPermission(context.getNodeRef(), PermissionService.READ_PERMISSIONS)
                    .equals(AccessStatus.ALLOWED)) {
                long aclId = AuthenticationUtil.runAsSystem(()->{
                    return serviceRegistry.getNodeService().getNodeAclId(context.getNodeRef());
                });
                Set<String> readers = serviceRegistry.getPermissionService().getReaders(aclId);
                context.getProperties().put(CCConstants.VIRT_PROP_GROUPS, StringUtils.join(readers
                        .stream().filter(a -> a.startsWith(AuthorityType.GROUP.getPrefixString())).map(a -> serviceRegistry.getAuthorityService().getAuthorityDisplayName(a)).collect(Collectors.toList()),CCConstants.MULTIVALUE_SEPARATOR));
            }
        }
        return context.getProperties();
    }
}
