package org.edu_sharing.alfresco.service.config.model;

import javax.xml.bind.annotation.XmlElement;
import java.io.Serializable;

public class License implements Serializable {
    @XmlElement public String id;
    @XmlElement public Integer position;
    @XmlElement public String url;
}
