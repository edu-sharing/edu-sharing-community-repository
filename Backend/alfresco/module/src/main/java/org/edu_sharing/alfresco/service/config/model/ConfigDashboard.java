package org.edu_sharing.alfresco.service.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlElement;

import java.util.List;

public class ConfigDashboard {
    @XmlElement public ShortcutConfig shortcuts = new ShortcutConfig();
    @Schema(description = "Swimlanes shown on the dashboard (in order). If not set, the frontend default (all swimlanes, expanded) is used")
    @JsonProperty("swimlanes")
    @XmlElement(name = "swimlane") public List<DashboardSwimlaneEntry> swimlanes;
}
