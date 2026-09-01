package org.edu_sharing.alfresco.service.config.model;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

public class ShortcutConfigEntry {
    @XmlElement public String id;
    @XmlElement public String icon;
    @XmlElement public String url;
    @XmlElement public String toolPermission;

    @XmlJavaTypeAdapter(ConfigVisibilityAdapter.class)
    @XmlElement public ConfigVisibility defaultVisibility;
}

