package org.edu_sharing.service.config.model;

import javax.xml.bind.annotation.XmlElement;
import java.io.Serializable;

public class SimpleEdit implements Serializable {
    @XmlElement
    public String[] globalGroups;
    @XmlElement
    public Organization organization;

    private class Organization implements Serializable{
        @XmlElement
        public String[] groupTypes;
    }
}
