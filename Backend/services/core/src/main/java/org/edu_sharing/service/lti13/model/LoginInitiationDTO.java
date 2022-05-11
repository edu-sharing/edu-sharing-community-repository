package org.edu_sharing.service.lti13.model;

public class LoginInitiationDTO {
    private String iss;
    private String loginHint;
    private String targetLinkUri;
    private String ltiMessageHint;
    private String clientId;
    private String deploymentId;


    public LoginInitiationDTO(String iss, String loginHint, String targetLinkUri, String ltiMessageHint, String clientId, String deploymentId) {
        this.iss = iss;
        this.loginHint = loginHint;
        this.targetLinkUri = targetLinkUri;
        this.ltiMessageHint = ltiMessageHint;
        this.clientId = clientId;
        this.deploymentId = deploymentId;
    }

    public String getIss() {
        return iss;
    }

    public void setIss(String iss) {
        this.iss = iss;
    }

    public String getLoginHint() {
        return loginHint;
    }

    public void setLoginHint(String loginHint) {
        this.loginHint = loginHint;
    }

    public String getTargetLinkUri() {
        return targetLinkUri;
    }

    public void setTargetLinkUri(String targetLinkUri) {
        this.targetLinkUri = targetLinkUri;
    }

    public String getLtiMessageHint() {
        return ltiMessageHint;
    }

    public void setLtiMessageHint(String ltiMessageHint) {
        this.ltiMessageHint = ltiMessageHint;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getDeploymentId() {
        return deploymentId;
    }

    public void setDeploymentId(String deploymentId) {
        this.deploymentId = deploymentId;
    }
}
