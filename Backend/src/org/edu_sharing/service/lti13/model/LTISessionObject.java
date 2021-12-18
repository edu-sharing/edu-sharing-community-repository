package org.edu_sharing.service.lti13.model;

import java.io.Serializable;
import java.util.Map;

/**
 * contains the information needed by later processes (deeplink, resourcelink)
 * keep in session
 */
public class LTISessionObject implements Serializable {



    String messageType;

    Map deepLinkingSettings;

    public void setDeepLinkingSettings(Map deepLinkingSettings) {
        this.deepLinkingSettings = deepLinkingSettings;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }
}
