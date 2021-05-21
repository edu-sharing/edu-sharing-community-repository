package org.edu_sharing.alfresco.service.config.model;

import javax.xml.bind.annotation.XmlElement;
import java.io.Serializable;

public class ConfigRemoteRocketchat implements Serializable {
    /**
     * Shall the chat window be opened (not minimized) after login?
     */
    @XmlElement
    boolean shouldOpen;
}
