package org.edu_sharing.service.config.model;

import javax.xml.bind.annotation.XmlElement;

public class Register {
    @XmlElement public Boolean local;
    @XmlElement public String loginUrl;
    @XmlElement public String recoverUrl;

}
