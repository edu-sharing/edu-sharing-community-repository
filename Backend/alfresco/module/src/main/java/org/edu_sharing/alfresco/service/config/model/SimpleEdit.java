package org.edu_sharing.alfresco.service.config.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlElement;
import java.io.Serializable;

public class SimpleEdit implements Serializable {
    @Schema(description = "Global groups to offer in quick edit dialog")
    @XmlElement
    public SimpleEditGlobalGroups[] globalGroups;
    @Schema(description = "Organization configuration for quick edit")
    @XmlElement
    public SimpleEditOrganization organization;
    @Schema(description = "Organization filter pattern")
    @XmlElement
    public String organizationFilter;

    @Schema(description = "Array of allowed license IDs for quick edit")
    @XmlElement
    public String[] licenses;


    private static class SimpleEditOrganization implements Serializable{
        @Schema(description = "Group types to include")
        @XmlElement
        public String[] groupTypes;
    }
}
