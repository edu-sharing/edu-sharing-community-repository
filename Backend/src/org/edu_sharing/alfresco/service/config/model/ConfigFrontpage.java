package org.edu_sharing.service.config.model;

import javax.xml.bind.annotation.XmlElement;
import java.io.Serializable;

public class ConfigFrontpage implements Serializable {
    @XmlElement public Boolean enabled;
}
