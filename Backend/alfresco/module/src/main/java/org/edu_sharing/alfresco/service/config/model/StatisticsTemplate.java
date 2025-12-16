package org.edu_sharing.alfresco.service.config.model;

import jakarta.xml.bind.annotation.XmlElement;
import java.io.Serializable;

public class StatisticsTemplate implements Serializable {
    @XmlElement public String name;
    @XmlElement public String group;
    @XmlElement public String unfold;
    @XmlElement public String type;
}
