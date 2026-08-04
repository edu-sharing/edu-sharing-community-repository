package org.edu_sharing.alfresco.service.config.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlElement;
import java.io.Serializable;

public class FontIcon implements Serializable {
    @Schema(description = "Original Material Design icon identifier to replace")
    @XmlElement
    public String original;
    @Schema(description = "Context in which this replacement applies, as a regular expression matched against the whole context reported by the frontend (a plain string therefore acts as an exact match, e.g. 'mds', 'option', 'collection-scope'; 'sidebar-.*' matches all sidebar contexts). Icons that report no context are matched against the empty string, so '.*' also covers them while '.+' requires a context; '(?!edge-toggle$|sidebar-navigate$).*' applies everywhere except those two contexts. Empty/null = applies to all contexts, but such an entry is only used if no entry with a matching context exists. If several entries match, the first one in the list wins")
    @XmlElement
    public String context;
    @Schema(description = "Replacement icon identifier or CSS class")
    @XmlElement public String replace;
    @Schema(description = "CSS class name")
    @XmlElement public String cssClass;
}