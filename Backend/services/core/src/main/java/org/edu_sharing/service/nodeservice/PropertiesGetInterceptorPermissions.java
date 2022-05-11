package org.edu_sharing.service.nodeservice;

import com.google.common.collect.Lists;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.permission.PermissionServiceHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PropertiesGetInterceptorPermissions extends PropertiesGetInterceptorDefault {
    public static Map<String, List<String>> PERMISSION_LIST = new HashMap<>();
    static {
        PERMISSION_LIST.put(CCConstants.PERMISSION_WRITE, Lists.newArrayList(
                CCConstants.CCM_PROP_WF_INSTRUCTIONS,
                CCConstants.CCM_PROP_WF_RECEIVER,
                CCConstants.CCM_PROP_WF_PROTOCOL,
                CCConstants.CCM_PROP_PH_ACTION,
                CCConstants.CCM_PROP_PH_HISTORY,
                CCConstants.CCM_PROP_PH_INVITED,
                CCConstants.CCM_PROP_PH_MODIFIED,
                CCConstants.CCM_PROP_PH_USERS
        ));
    }
    @Override
    public Map<String, Object> beforeDeliverProperties(PropertiesContext context) {
        boolean write = PermissionServiceHelper.hasPermission(context.getNodeRef(), CCConstants.PERMISSION_WRITE);
        if(!write) {
            PERMISSION_LIST.get(CCConstants.PERMISSION_WRITE).forEach((p) -> {
                context.getProperties().remove(p);
            });
        }
        return context.getProperties();
    }

}
