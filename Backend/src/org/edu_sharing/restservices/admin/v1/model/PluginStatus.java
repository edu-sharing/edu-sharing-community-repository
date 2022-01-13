package org.edu_sharing.restservices.admin.v1.model;

import io.swagger.annotations.ApiModel;

@ApiModel
public class PluginStatus {
    private String name;
    private Boolean enabled;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public PluginStatus(
            String name,
            Boolean enabled
    ) {

        this.name = name;
        this.enabled = enabled;
    }
}
