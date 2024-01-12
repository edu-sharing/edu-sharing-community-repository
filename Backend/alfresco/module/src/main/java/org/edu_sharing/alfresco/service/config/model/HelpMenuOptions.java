package org.edu_sharing.alfresco.service.config.model;

import jakarta.xml.bind.annotation.XmlElement;
import java.io.Serializable;

public class HelpMenuOptions implements Serializable {
    @XmlElement public String key;
    @XmlElement public String icon;
    @XmlElement public String url;
}
