package org.edu_sharing.alfresco.service.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.xml.bind.annotation.XmlElement;

import java.util.ArrayList;
import java.util.List;

public class ShortcutConfig {
    @XmlElement public int maxEntries;
    @JsonProperty("entries")
    @XmlElement(name = "entry") public List<ShortcutConfigEntry> entries = new ArrayList<>();
}
