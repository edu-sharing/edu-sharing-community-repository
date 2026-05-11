package org.edu_sharing.alfresco.service.config.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlElement;
import java.io.Serializable;

public class Stream implements Serializable {
    @Schema(description = "If true, enable activity stream/feed feature (default: false)")
    @XmlElement public boolean enabled;
}
