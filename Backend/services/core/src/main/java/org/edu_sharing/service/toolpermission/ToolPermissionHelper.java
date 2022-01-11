package org.edu_sharing.service.toolpermission;

import org.edu_sharing.alfresco.service.toolpermission.ToolPermissionException;

public class ToolPermissionHelper {
    public static void throwIfToolpermissionMissing(String toolpermission){
        if(!ToolPermissionServiceFactory.getInstance().hasToolPermission(toolpermission)){
            throw new ToolPermissionException(toolpermission);
        }
    }
}
