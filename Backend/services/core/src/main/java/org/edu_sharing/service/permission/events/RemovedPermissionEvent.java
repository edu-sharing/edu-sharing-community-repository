package org.edu_sharing.service.permission.events;

import org.alfresco.service.cmr.security.AccessPermission;

import java.util.Set;

public record RemovedPermissionEvent(String node_id, String creator, Set<AccessPermission> permissions) {

}
