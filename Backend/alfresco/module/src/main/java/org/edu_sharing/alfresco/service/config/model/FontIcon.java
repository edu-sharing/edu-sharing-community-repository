package org.edu_sharing.alfresco.service.config.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlElement;
import java.io.Serializable;

public class FontIcon implements Serializable {
    @Schema(description = "Original Material Design icon identifier to replace")
    @XmlElement
    public String original;
    @Schema(description = "Context for this replacement (null = all contexts)")
    @XmlElement
    public String context;
    @Schema(description = "Replacement icon identifier or CSS class")
    @XmlElement public String replace;
    @Schema(description = "CSS class name")
    @XmlElement public String cssClass;
}