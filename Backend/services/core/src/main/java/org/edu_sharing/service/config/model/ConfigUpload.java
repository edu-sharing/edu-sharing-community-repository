package org.edu_sharing.service.config.model;

import javax.xml.bind.annotation.XmlElement;
import java.io.Serializable;

public class ConfigUpload implements Serializable {
    public static class ConfigUploadLti implements Serializable {
        @XmlElement Boolean enabled;
    }
    @XmlElement ConfigUploadLti lti;

}
