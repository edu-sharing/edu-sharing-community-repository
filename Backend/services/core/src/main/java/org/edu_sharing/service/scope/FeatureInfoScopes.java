package org.edu_sharing.service.scope;

import org.edu_sharing.restservices.about.v1.model.FeatureInfo;
import org.edu_sharing.spring.conditions.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "repository.scopes.enabled", havingValue = "true")
public class FeatureInfoScopes implements FeatureInfo {

    @Override
    public Features getId() {
        return Features.scopes;
    }
}
