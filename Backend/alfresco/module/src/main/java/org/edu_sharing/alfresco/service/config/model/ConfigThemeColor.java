package org.edu_sharing.alfresco.service.config.model;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;
import java.io.Serializable;

public class ConfigThemeColor implements Serializable {
    @XmlAttribute
    public String variable;
    @XmlValue
    public String value;
}
