package org.edu_sharing.alfresco.service.config.model;

import javax.xml.bind.annotation.XmlElement;
import java.io.Serializable;

public class StatisticsTemplate implements Serializable {
    @XmlElement String name;
    @XmlElement String group;
    @XmlElement String unfold;
    @XmlElement String type;
}
