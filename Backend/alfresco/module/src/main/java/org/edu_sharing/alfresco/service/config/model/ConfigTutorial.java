package org.edu_sharing.alfresco.service.config.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlElement;

public class ConfigTutorial {
    @Schema(description = "If true (default), show frontend tutorial with darkened area and highlighted elements")
    @XmlElement
    public boolean enabled;
}
