package org.edu_sharing.alfresco.monitoring;

import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

@ManagedResource(objectName = "EduSharing:type=ApplicationInfo")
public class ApplicationInfoMBeanImpl extends MBeanSupport implements ApplicationInfoMBean {

    @ManagedAttribute
    @Override
    public int getCount() {
        return doWork(() -> ApplicationInfoList.getApplicationInfos().size());
    }
}
