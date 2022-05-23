package org.edu_sharing.restservices.shared;

public class NodeLTIDeepLink {
    String ltiDeepLinkReturnUrl;
    String jwtDeepLinkResponse;

    public NodeLTIDeepLink(String ltiDeepLinkReturnUrl, String jwtDeepLinkResponse){
        this.ltiDeepLinkReturnUrl = ltiDeepLinkReturnUrl;
        this.jwtDeepLinkResponse = jwtDeepLinkResponse;
    }

    public String getJwtDeepLinkResponse() {
        return jwtDeepLinkResponse;
    }

    public void setJwtDeepLinkResponse(String jwtDeepLinkResponse) {
        this.jwtDeepLinkResponse = jwtDeepLinkResponse;
    }

    public String getLtiDeepLinkReturnUrl() {
        return ltiDeepLinkReturnUrl;
    }

    public void setLtiDeepLinkReturnUrl(String ltiDeepLinkReturnUrl) {
        this.ltiDeepLinkReturnUrl = ltiDeepLinkReturnUrl;
    }
}
