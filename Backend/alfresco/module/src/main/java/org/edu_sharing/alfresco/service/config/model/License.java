package org.edu_sharing.alfresco.service.config.model;

import jakarta.xml.bind.annotation.XmlElement;
import java.io.Serializable;

public class License implements Serializable {
    @XmlElement public String id;
    @XmlElement public Integer position;
    @XmlElement public String url;
}
