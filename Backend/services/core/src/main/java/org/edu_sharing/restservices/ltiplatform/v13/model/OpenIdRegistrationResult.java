package org.edu_sharing.restservices.ltiplatform.v13.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class OpenIdRegistrationResult {
    String client_id;
    List<String> response_types;
    String jwks_uri;
    String initiate_login_uri;
    List<String> grant_types;
    List<String> redirect_uris;
    String application_type;
    String token_endpoint_auth_method;
    String client_name;
    String logo_uri;
    String scope;

    @JsonProperty("https://purl.imsglobal.org/spec/lti-tool-configuration")
    LTIToolConfiguration ltiToolConfiguration;

    public String getClient_id() {
        return client_id;
    }

    public void setClient_id(String client_id) {
        this.client_id = client_id;
    }

    public List<String> getResponse_types() {
        return response_types;
    }

    public void setResponse_types(List<String> response_types) {
        this.response_types = response_types;
    }

    public String getJwks_uri() {
        return jwks_uri;
    }

    public void setJwks_uri(String jwks_uri) {
        this.jwks_uri = jwks_uri;
    }

    public String getInitiate_login_uri() {
        return initiate_login_uri;
    }

    public void setInitiate_login_uri(String initiate_login_uri) {
        this.initiate_login_uri = initiate_login_uri;
    }

    public List<String> getGrant_types() {
        return grant_types;
    }

    public void setGrant_types(List<String> grant_types) {
        this.grant_types = grant_types;
    }

    public List<String> getRedirect_uris() {
        return redirect_uris;
    }

    public void setRedirect_uris(List<String> redirect_uris) {
        this.redirect_uris = redirect_uris;
    }

    public String getApplication_type() {
        return application_type;
    }

    public void setApplication_type(String application_type) {
        this.application_type = application_type;
    }

    public String getToken_endpoint_auth_method() {
        return token_endpoint_auth_method;
    }

    public void setToken_endpoint_auth_method(String token_endpoint_auth_method) {
        this.token_endpoint_auth_method = token_endpoint_auth_method;
    }

    public String getClient_name() {
        return client_name;
    }

    public void setClient_name(String client_name) {
        this.client_name = client_name;
    }

    public String getLogo_uri() {
        return logo_uri;
    }

    public void setLogo_uri(String logo_uri) {
        this.logo_uri = logo_uri;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public LTIToolConfiguration getLtiToolConfiguration() {
        return ltiToolConfiguration;
    }

    public void setLtiToolConfiguration(LTIToolConfiguration ltiToolConfiguration) {
        this.ltiToolConfiguration = ltiToolConfiguration;
    }

    public static class LTIToolConfiguration{
        String version;
        String deployment_id;
        String target_link_uri;
        String domain;
        String description;
        List<String> claims;

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getDeployment_id() {
            return deployment_id;
        }

        public void setDeployment_id(String deployment_id) {
            this.deployment_id = deployment_id;
        }

        public String getTarget_link_uri() {
            return target_link_uri;
        }

        public void setTarget_link_uri(String target_link_uri) {
            this.target_link_uri = target_link_uri;
        }

        public String getDomain() {
            return domain;
        }

        public void setDomain(String domain) {
            this.domain = domain;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public List<String> getClaims() {
            return claims;
        }

        public void setClaims(List<String> claims) {
            this.claims = claims;
        }

    }

}
