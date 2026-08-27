package org.edu_sharing.alfresco.service.config.model;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

public class ConfigVisibilityAdapter extends XmlAdapter<String, ConfigVisibility> {
    @Override
    public ConfigVisibility unmarshal(String v) {
        for (ConfigVisibility visibility : ConfigVisibility.values()) {
            if (visibility.toString().equalsIgnoreCase(v)) {
                return visibility;
            }
        }
        throw new IllegalArgumentException("Unknown visibility: " + v);
    }

    @Override
    public String marshal(ConfigVisibility v) {
        return v.toString();
    }
}
