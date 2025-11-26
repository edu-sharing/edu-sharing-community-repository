package org.edu_sharing.restservices.login.v1.model;

import lombok.Data;

import java.util.List;

@Data
public class OAuth2Consent {
    public static final String SESS_ATT_CLIENT_ID = "consent.client_id";
    public static final String SESS_ATT_STATE = "consent.state";
    public static final String SESS_ATT_SCOPES = "consent.scopes";
    String clientId;
    String state;
    List<String> scopes;
}
