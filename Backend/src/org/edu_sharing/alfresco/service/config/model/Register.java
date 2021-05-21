package org.edu_sharing.alfresco.service.config.model;

import javax.xml.bind.annotation.XmlElement;
import java.io.Serializable;
import java.util.List;

public class Register implements Serializable {
    @XmlElement public Boolean local;
    @XmlElement public String loginUrl;
    @XmlElement public String recoverUrl;
    @XmlElement public List<String> requiredFields;

}
