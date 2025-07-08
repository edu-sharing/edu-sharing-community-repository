package org.edu_sharing.alfresco.service.config.model;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.xml.bind.annotation.XmlElement;
import java.io.Serializable;

public class FontIcon implements Serializable {
    @XmlElement
    public String original;
    @JsonPropertyDescription("Context for this icon to replace. When null, all contexts are replaced")
    @XmlElement
    public String context;
    @XmlElement public String replace;
    @XmlElement public String cssClass;
}