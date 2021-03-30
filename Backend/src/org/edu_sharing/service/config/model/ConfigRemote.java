package org.edu_sharing.service.config.model;

import javax.xml.bind.annotation.XmlElement;
import java.io.Serializable;

public class ConfigRemote implements Serializable {

    public static class ConfigRemoteRocketchat implements Serializable{
        /**
         * Shall the chat window be opened (not minimized) after login?
         */
        @XmlElement boolean shouldOpen;
    }

    @XmlElement
    public ConfigRemoteRocketchat rocketchat;

    public enum ConfigUploadDialog {
        SimpleEdit,
        Mds
    }
}
