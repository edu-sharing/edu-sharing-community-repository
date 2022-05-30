package org.edu_sharing.alfresco.service.config.model;

import javax.xml.bind.annotation.XmlElement;
import java.io.Serializable;

public class Statistics implements Serializable {
    @XmlElement public String[] groups;
    @XmlElement public String nodeGroup;
    @XmlElement String[] nodeColumns;
    @XmlElement StatisticsTemplate[] templates;
}
