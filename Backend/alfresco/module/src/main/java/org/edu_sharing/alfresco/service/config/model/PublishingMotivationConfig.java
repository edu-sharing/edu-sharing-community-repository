package org.edu_sharing.alfresco.service.config.model;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.xml.bind.annotation.XmlElement;

import java.io.Serializable;
import java.util.List;

public class PublishingMotivationConfig implements Serializable {
    @XmlElement
    public boolean confetti;
    @JsonPropertyDescription("at which counts of published materials the message will be shown")
    @XmlElement
    public List<Integer> range;
}
