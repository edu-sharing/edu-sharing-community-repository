package org.edu_sharing.alfresco.service.config.model;

import javax.xml.bind.annotation.XmlElement;
import java.io.Serializable;

public class FontIcon implements Serializable {
    @XmlElement
    public String original;
    @XmlElement public String replace;
}