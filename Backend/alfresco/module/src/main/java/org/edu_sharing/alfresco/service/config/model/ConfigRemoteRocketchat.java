package org.edu_sharing.alfresco.service.config.model;

import jakarta.xml.bind.annotation.XmlElement;
import java.io.Serializable;

public class ConfigRemoteRocketchat implements Serializable {
    /**
     * Shall the chat window be opened (not minimized) after login?
     */
    @XmlElement
    boolean shouldOpen;
}
