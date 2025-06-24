package org.edu_sharing.alfresco.service.config.model;

import jakarta.xml.bind.annotation.XmlElement;

public class ConfigDashboard {
    @XmlElement public ShortcutConfig shortcuts = new ShortcutConfig();
}

