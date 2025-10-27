package org.edu_sharing.spring.security.openid.persistence;



public class OidcUserSessionRecord {
    private String sessionId;
    private OidcSessionInformationDto sessionInformation;

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public OidcSessionInformationDto getSessionInformation() {
        return sessionInformation;
    }

    public void setSessionInformation(OidcSessionInformationDto sessionInformation) {
        this.sessionInformation = sessionInformation;
    }
}
