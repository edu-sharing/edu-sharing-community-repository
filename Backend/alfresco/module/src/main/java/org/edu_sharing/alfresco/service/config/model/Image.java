package org.edu_sharing.alfresco.service.config.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlElement;
import java.io.Serializable;

public class Image implements Serializable {
    @Schema(description = "Original image URL to match (matched with endsWith)")
    @XmlElement public String src;
    @Schema(description = "Replacement image URL")
    @XmlElement public String replace;
}
