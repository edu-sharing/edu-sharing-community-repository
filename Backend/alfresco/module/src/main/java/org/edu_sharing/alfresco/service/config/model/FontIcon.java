package org.edu_sharing.alfresco.service.config.model;

import jakarta.xml.bind.annotation.XmlElement;
import java.io.Serializable;

public class FontIcon implements Serializable {
    @XmlElement
    public String original;
    @XmlElement public String replace;
    @XmlElement public String cssClass;
}