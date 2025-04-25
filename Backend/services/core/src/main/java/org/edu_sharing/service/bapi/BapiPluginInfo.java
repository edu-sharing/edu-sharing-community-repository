package org.edu_sharing.service.bapi;

import org.edu_sharing.restservices.about.v1.model.PluginInfo;
import org.edu_sharing.spring.conditions.ConditionalOnProperty;
import org.springframework.stereotype.Component;


@Component
@ConditionalOnProperty(name = "repository.bapi.uri")
public class BapiPluginInfo implements PluginInfo {
    @Override
    public String getId() {
        return "b-api";
    }
}
