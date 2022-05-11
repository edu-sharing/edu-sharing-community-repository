package org.edu_sharing.alfresco.service.config.model;

import javax.xml.bind.annotation.XmlElement;
import java.io.Serializable;

public class ConfigRemote implements Serializable {

    @XmlElement
    public ConfigRemoteRocketchat rocketchat;

    public enum ConfigUploadDialog {
        SimpleEdit,
        Mds
    }
}
