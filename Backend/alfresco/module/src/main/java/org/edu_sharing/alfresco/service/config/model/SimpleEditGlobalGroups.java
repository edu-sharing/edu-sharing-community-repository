package org.edu_sharing.alfresco.service.config.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlElement;
import java.io.Serializable;

public class SimpleEditGlobalGroups implements Serializable {
    @Schema(description = "Tool permission required for this group entry (optional, applies to all if not set)")
    @XmlElement
    public String toolpermission;
    @Schema(description = "Array of group IDs to offer in quick edit dialog")
    @XmlElement
    public String[] groups;
}
