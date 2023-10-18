package org.edu_sharing.alfresco.service;

import org.alfresco.service.cmr.repository.NodeRef;

public interface EduSharingCustomPermissionService {
    public EsAccessStatus hasPermission(NodeRef nodeRef);

    enum EsAccessStatus {

        // access is granted
        ALLOWED,
        // access is denied
        DENIED,
        // access is enforced to be denied, i.e. triggered by an interceptor. It
        // should not be allowed to grant access afterwards
        DENIED_ENFORCE
    }
}
