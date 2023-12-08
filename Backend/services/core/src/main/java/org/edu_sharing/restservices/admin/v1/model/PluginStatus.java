package org.edu_sharing.restservices.admin.v1.model;
public class PluginStatus {
    private String version;
    private String name;
    private Boolean enabled;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public PluginStatus(
            String name,
            String version,
            Boolean enabled
    ) {

        this.name = name;
        this.version = version;
        this.enabled = enabled;
    }
}
