package org.edu_sharing.alfresco.service.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlElement;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ConfigWorkspaceColumns implements Serializable {
    @Schema(description = "Columns available in the workspace. A column not listed here is not offered in the column chooser. The order defines the column order")
    @JsonProperty("columns")
    @XmlElement(name = "column") public List<WorkspaceColumnConfigEntry> columns = new ArrayList<>();

    @Schema(description = "Per base area overrides. An area not listed here uses the global column list")
    @JsonProperty("areas")
    @XmlElement(name = "area") public List<ConfigWorkspaceColumnsArea> areas = new ArrayList<>();
}
