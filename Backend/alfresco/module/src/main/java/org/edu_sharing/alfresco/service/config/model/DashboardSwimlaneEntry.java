package org.edu_sharing.alfresco.service.config.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlElement;

import java.io.Serializable;

public class DashboardSwimlaneEntry implements Serializable {
    @Schema(description = "Swimlane type")
    @XmlElement public DashboardSwimlaneType id;
    @Schema(description = "Whether the swimlane is expanded initially (the user's own toggle state takes precedence)")
    @XmlElement public Boolean defaultExpanded;
}
