package org.edu_sharing.alfresco.service.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlElement;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ConfigWorkspaceColumnsArea implements Serializable {
    @Schema(description = "Workspace base area this override applies to: 'MY_FILES', 'SHARED_FILES' or 'WORKFLOW_RECEIVE'")
    @XmlElement public String root;

    @Schema(description = "Columns available in this area. Replaces the global column list entirely")
    @JsonProperty("columns")
    @XmlElement(name = "column") public List<WorkspaceColumnConfigEntry> columns = new ArrayList<>();
}
