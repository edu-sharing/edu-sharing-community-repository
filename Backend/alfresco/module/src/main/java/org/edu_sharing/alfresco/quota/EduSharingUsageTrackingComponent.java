package org.edu_sharing.alfresco.quota;

import lombok.extern.slf4j.Slf4j;
import org.alfresco.repo.usage.UserUsageTrackingComponent;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;

@Slf4j
public class EduSharingUsageTrackingComponent extends UserUsageTrackingComponent {

    @Override
    public void bootstrapInternal() {
        boolean calculateMissingUsages = LightbendConfigLoader.get().getBoolean("repository.quota.calculateMissingUsages");
        if(calculateMissingUsages){
            super.bootstrapInternal();
        }else {
            log.info("calculateMissingUsages at startup is disabled");
        }
    }
}
