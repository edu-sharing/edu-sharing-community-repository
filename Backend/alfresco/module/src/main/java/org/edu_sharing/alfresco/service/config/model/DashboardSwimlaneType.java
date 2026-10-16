package org.edu_sharing.alfresco.service.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.xml.bind.annotation.XmlEnumValue;

public enum DashboardSwimlaneType {
    @XmlEnumValue("featured-media") @JsonProperty("featured-media") featuredMedia,
    @XmlEnumValue("collections") @JsonProperty("collections") collections,
    @XmlEnumValue("recent-activities") @JsonProperty("recent-activities") recentActivities,
    @XmlEnumValue("shares") @JsonProperty("shares") shares,
    @XmlEnumValue("assignments") @JsonProperty("assignments") assignments
}
