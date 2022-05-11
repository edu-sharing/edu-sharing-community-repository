package org.edu_sharing.alfresco.service.config.model;

import javax.xml.bind.annotation.XmlElement;
import java.io.Serializable;

public class HelpMenuOptions implements Serializable {
    @XmlElement public String key;
    @XmlElement public String icon;
    @XmlElement public String url;
}
