package org.edu_sharing.alfresco.service.config.model;

import jakarta.xml.bind.annotation.XmlElement;
import java.io.Serializable;

public class ConfigUpload implements Serializable {
    @XmlElement
    public ConfigUploadDialog postDialog;

    public enum ConfigUploadDialog {
        SimpleEdit,
        Mds
    }
}
