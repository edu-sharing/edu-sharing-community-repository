package org.edu_sharing.alfresco.service.config.model;

import jakarta.xml.bind.annotation.XmlElement;
import java.io.Serializable;
import java.util.List;

public class ConfigThemeColors implements Serializable {
    public @XmlElement List<ConfigThemeColor> color;
}
