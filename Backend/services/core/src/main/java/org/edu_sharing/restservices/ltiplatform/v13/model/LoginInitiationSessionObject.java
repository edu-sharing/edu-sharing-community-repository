package org.edu_sharing.restservices.ltiplatform.v13.model;

public class LoginInitiationSessionObject {

    public static enum MessageType{resourcelink,deeplink}

    String contextId,clientId,appId,nodeId,resourceLinkNodeId;

    MessageType messageType;

    public String getContextId() {
        return contextId;
    }

    public void setContextId(String contextId) {
        this.contextId = contextId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getNodeId() {
        return nodeId;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public void setResourceLinkNodeId(String resourceLinkNodeId) {
        this.resourceLinkNodeId = resourceLinkNodeId;
    }

    public String getResourceLinkNodeId() {
        return resourceLinkNodeId;
    }
}
