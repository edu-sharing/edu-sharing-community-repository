package org.edu_sharing.alfresco.service.config.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlElement;
import java.io.Serializable;

public class License implements Serializable {
    @Schema(description = "License identifier (recommended: UPPERCASE)")
    @XmlElement public String id;
    @Schema(description = "Position in list (negative = count from end)")
    @XmlElement public Integer position;
    @Schema(description = "URL to license description (prepared but not yet used)")
    @XmlElement public String url;
}
