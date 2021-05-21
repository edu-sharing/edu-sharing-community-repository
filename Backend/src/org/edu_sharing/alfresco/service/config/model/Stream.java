package org.edu_sharing.service.config.model;

import javax.xml.bind.annotation.XmlElement;
import java.io.Serializable;

public class Stream implements Serializable {
    @XmlElement public boolean enabled;
}
