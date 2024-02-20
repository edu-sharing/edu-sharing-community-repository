package org.edu_sharing.alfresco.service.config.model;

import jakarta.xml.bind.annotation.XmlElement;
import java.io.Serializable;

public class ConfigRemote implements Serializable {

    @XmlElement
    public ConfigRemoteRocketchat rocketchat;

    public enum ConfigUploadDialog {
        SimpleEdit,
        Mds
    }
}
