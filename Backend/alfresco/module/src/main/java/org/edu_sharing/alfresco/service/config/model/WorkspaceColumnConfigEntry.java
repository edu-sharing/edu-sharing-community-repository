package org.edu_sharing.alfresco.service.config.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import java.io.Serializable;

public class WorkspaceColumnConfigEntry implements Serializable {
    @Schema(description = "Property name of the column, e.g. 'cm:name' or 'ccm:university'. Any node property is allowed. The column label is resolved via the i18n key 'NODE.<id>'")
    @XmlElement public String id;

    @Schema(description = "Whether the column is displayed by default ('visible') or only offered in the column chooser ('hidden')")
    @XmlJavaTypeAdapter(ConfigVisibilityAdapter.class)
    @XmlElement public ConfigVisibility defaultVisibility;
}
