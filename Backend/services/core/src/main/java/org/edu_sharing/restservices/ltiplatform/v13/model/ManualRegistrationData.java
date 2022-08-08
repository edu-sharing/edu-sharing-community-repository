package org.edu_sharing.restservices.ltiplatform.v13.model;

import io.swagger.annotations.ApiModelProperty;

import java.util.List;

public class ManualRegistrationData {

    String toolName;

    String toolUrl;

    String toolDescription;

    String keysetUrl;

    String loginInitiationUrl;

    List<String> redirectionUrls;

    @ApiModelProperty(notes = "JSON Object where each value is a string. Custom parameters to be included in each launch to this tool. If a custom parameter is also defined at the message level, the message level value takes precedence. The value of the custom parameters may be substitution parameters as described in the LTI Core [LTI-13] specification. ", required = false)
    List<String> customParameters;

    String logoUrl;

    @ApiModelProperty(notes = "The default target link uri to use unless defined otherwise in the message or link definition",required = true)
    String targetLinkUri;

    @ApiModelProperty(name = "client_name", notes = "Name of the Tool to be presented to the End-User. Localized representations may be included as described in Section 2.1 of the [OIDC-Reg] specification. ",required = true)
    String clientName;

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public String getToolUrl() {
        return toolUrl;
    }

    public void setToolUrl(String toolUrl) {
        this.toolUrl = toolUrl;
    }

    public String getToolDescription() {
        return toolDescription;
    }

    public void setToolDescription(String toolDescription) {
        this.toolDescription = toolDescription;
    }

    public String getKeysetUrl() {
        return keysetUrl;
    }

    public void setKeysetUrl(String keysetUrl) {
        this.keysetUrl = keysetUrl;
    }

    public String getLoginInitiationUrl() {
        return loginInitiationUrl;
    }

    public void setLoginInitiationUrl(String loginInitiationUrl) {
        this.loginInitiationUrl = loginInitiationUrl;
    }

    public List<String> getRedirectionUrls() {
        return redirectionUrls;
    }

    public void setRedirectionUrls(List<String> redirectionUrls) {
        this.redirectionUrls = redirectionUrls;
    }

    public List<String> getCustomParameters() {
        return customParameters;
    }

    public void setCustomParameters(List<String> customParameters) {
        this.customParameters = customParameters;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setTargetLinkUri(String targetLinkUri) {
        this.targetLinkUri = targetLinkUri;
    }

    public String getTargetLinkUri() {
        return targetLinkUri;
    }


    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getClientName() {
        return clientName;
    }
}
