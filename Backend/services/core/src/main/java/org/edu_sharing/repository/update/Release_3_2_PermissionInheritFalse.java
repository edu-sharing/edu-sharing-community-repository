package org.edu_sharing.repository.update;

import lombok.extern.slf4j.Slf4j;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.PermissionService;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.update.UpdateRoutine;
import org.edu_sharing.repository.server.update.UpdateService;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@UpdateService
public class Release_3_2_PermissionInheritFalse {

    public static String ID = "Release_3_2_PermissionInheritFalse";

    public static String description = "sets inherit to false on User Homes";
    private final PermissionService permissionService;


    @Autowired
    public Release_3_2_PermissionInheritFalse(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @UpdateRoutine(
            id = "Release_3_2_PermissionInheritFalse",
            description = "sets inherit to false on User Homes",
            order = 3202,
            auto = true
    )
    public void execute() {
        NodeRef nodeRefUserHome = new MCAlfrescoAPIClient().getUserHomesNodeRef(MCAlfrescoAPIClient.storeRef);
        permissionService.setInheritParentPermissions(nodeRefUserHome, false);
    }
}
