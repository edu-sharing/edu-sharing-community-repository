package org.edu_sharing.alfresco.service.config.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlElement;
import java.io.Serializable;

public class HelpMenuOptions implements Serializable {
    @Schema(description = "Button ID (used in translation: HELP.key or related)")
    @XmlElement public String key;
    @Schema(description = "Material Design icon identifier")
    @XmlElement public String icon;
    @Schema(description = "URL to open on button click")
    @XmlElement public String url;
}
