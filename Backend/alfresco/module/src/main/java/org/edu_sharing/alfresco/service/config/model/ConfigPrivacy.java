package org.edu_sharing.alfresco.service.config.model;

import javax.xml.bind.annotation.XmlElement;
import java.io.Serializable;

public class ConfigPrivacy implements Serializable {
    @XmlElement
    public boolean cookieDisclaimer;
}
