package org.edu_sharing.alfresco.monitoring;

import org.edu_sharing.repository.server.tools.ActionObserver;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

@ManagedResource(objectName = "EduSharing:type=PreviewAction")
public class PreviewActionMBeanImpl extends MBeanSupport implements PreviewActionMBean {

    @ManagedAttribute
    @Override
    public int getCount() {
        return doWork(() -> ActionObserver.getInstance().nodeActionsMap.size());
    }
}
