package org.edu_sharing.alfresco.service.config.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlElement;

import java.io.Serializable;

public class Gdpr implements Serializable {
    @Schema(description = "If true, enable GDPR-related features")
    @XmlElement public boolean enabled;
    @Schema(description = "GDPR entry definitions")
    @XmlElement public GdprEntry[] entry;
}
