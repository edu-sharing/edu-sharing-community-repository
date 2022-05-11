package org.edu_sharing.service.lti13.model;

import java.io.Serializable;
import java.util.Map;

/**
 * contains the information needed by later processes (deeplink, resourcelink)
 * keep in session
 */
public class LTISessionObject implements Serializable {


    String eduSharingAppId;

    String messageType;

    Map deepLinkingSettings;

    String nonce;

    String iss;

    String deploymentId;

    /**
     * i.e. lms courseId
     */
    String contextId;

    public void setDeepLinkingSettings(Map deepLinkingSettings) {
        this.deepLinkingSettings = deepLinkingSettings;
    }

    public Map getDeepLinkingSettings() {
        return deepLinkingSettings;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setEduSharingAppId(String eduSharingAppId) {
        this.eduSharingAppId = eduSharingAppId;
    }

    public String getEduSharingAppId() {
        return eduSharingAppId;
    }

    public String getNonce() {return nonce;}

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public String getIss() {
        return iss;
    }

    public void setIss(String iss) {
        this.iss = iss;
    }

    public String getDeploymentId() {
        return deploymentId;
    }

    public void setDeploymentId(String deploymentId) {
        this.deploymentId = deploymentId;
    }

    public void setContextId(String courseId) {
        this.contextId = courseId;
    }

    public String getContextId() {
        return contextId;
    }
}
