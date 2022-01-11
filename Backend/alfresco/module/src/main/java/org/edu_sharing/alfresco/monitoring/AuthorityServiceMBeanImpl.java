package org.edu_sharing.alfresco.monitoring;

import org.alfresco.service.cmr.security.AuthorityService;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

@ManagedResource(objectName = "Alfresco:type=AuthorityService")
public class AuthorityServiceMBeanImpl extends MBeanSupport implements AuthorityServiceMBean {

    private AuthorityService authorityService;

    public void setAuthorityService(AuthorityService authorityService) {
        this.authorityService = authorityService;
    }

    @Override
    @ManagedAttribute
    public int getGroupCount() { return doWork(() -> (int) authorityService.countGroups()); }

    @Override
    @ManagedAttribute
    public int getUserCount() {
        return doWork(() -> (int) authorityService.countUsers());
    }

}
