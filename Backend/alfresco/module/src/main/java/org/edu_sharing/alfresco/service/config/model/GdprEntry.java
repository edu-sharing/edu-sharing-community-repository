package org.edu_sharing.alfresco.service.config.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlElement;
import java.io.Serializable;

public class GdprEntry implements Serializable {
    @Schema(description = "Regex pattern to match data types")
    @XmlElement public String regex;
    @Schema(description = "Display name for this GDPR entry")
    @XmlElement public String name;
    @Schema(description = "Reference identifier")
    @XmlElement public String ref;
}
