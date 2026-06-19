package org.edu_sharing.alfresco.service.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlElement;

import java.io.Serializable;

public class Admin implements Serializable {
    @Schema(description = "Statistics configuration")
    @XmlElement public Statistics statistics;
    @JsonProperty
    @Schema(description = "Code editor type for config file editing: Textarea or Monaco")
    @XmlElement
    public EditorType editorType;

    public enum EditorType {
        Textarea,
        Monaco
    }
    @JsonProperty
    @Schema(description = "WYSIWYG editor type for message editing: Textarea or TinyMCE")
    @XmlElement
    public WysiwygType wysiwygType;

    public enum WysiwygType {
        Textarea,
        TinyMCE
    }

}
