package org.edu_sharing.alfresco.service.config.model;

import jakarta.xml.bind.annotation.XmlElement;
import java.io.Serializable;

public class GdprEntry implements Serializable {
    @XmlElement public String regex;
    @XmlElement public String name;
    @XmlElement public String ref;
}
