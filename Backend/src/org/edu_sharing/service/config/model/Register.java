package org.edu_sharing.service.config.model;

import javax.xml.bind.annotation.XmlElement;
import java.io.Serializable;

public class Register implements Serializable {
    @XmlElement public Boolean local;
    @XmlElement public String loginUrl;
    @XmlElement public String recoverUrl;

}
