package org.edu_sharing.service.dataprotection;

import org.edu_sharing.restservices.about.v1.model.FeatureInfo;
import org.edu_sharing.spring.conditions.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "repository.dataprotection.enabled", havingValue = "true")
public class FeatureInfoDataProtectionService implements FeatureInfo{

    @Override
    public FeatureInfo.Features getId() {
        return Features.dataprotection;
    }
}
