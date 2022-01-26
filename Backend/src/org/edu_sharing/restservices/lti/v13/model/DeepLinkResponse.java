package org.edu_sharing.restservices.lti.v13.model;

public class DeepLinkResponse {
    public String jwt;
    public String ltiDeepLinkReturnUrl;

    public String getLtiDeepLinkReturnUrl() {
        return ltiDeepLinkReturnUrl;
    }

    public void setLtiDeepLinkReturnUrl(String ltiDeepLinkReturnUrl) {
        this.ltiDeepLinkReturnUrl = ltiDeepLinkReturnUrl;
    }

    public String getJwt() {
        return jwt;
    }

    public void setJwt(String jwt) {
        this.jwt = jwt;
    }
}
