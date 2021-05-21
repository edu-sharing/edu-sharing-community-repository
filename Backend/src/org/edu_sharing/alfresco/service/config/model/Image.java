package org.edu_sharing.alfresco.service.config.model;

import javax.xml.bind.annotation.XmlElement;
import java.io.Serializable;

public class Image implements Serializable {
    @XmlElement public String src;
    @XmlElement public String replace;
}
