package org.edu_sharing.alfresco.service.config.model;

import jakarta.xml.bind.annotation.XmlElement;
import java.io.Serializable;

public class Image implements Serializable {
    @XmlElement public String src;
    @XmlElement public String replace;
}
