package org.edu_sharing.alfresco.service.config.model;

import jakarta.xml.bind.annotation.XmlElement;
import java.io.Serializable;

public class SimpleEdit implements Serializable {
    @XmlElement
    public SimpleEditGlobalGroups[] globalGroups;
    @XmlElement
    public SimpleEditOrganization organization;
    @XmlElement
    public String organizationFilter;

    @XmlElement
    public String[] licenses;


    private static class SimpleEditOrganization implements Serializable{
        @XmlElement
        public String[] groupTypes;
    }
}
