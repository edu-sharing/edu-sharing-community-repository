package org.edu_sharing.restservices.ltiplatform.v13.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class OpenIdConfiguration {

    String issuer;

    String token_endpoint;

    List<String> token_endpoint_auth_methods_supported;

    List<String> token_endpoint_auth_signing_alg_values_supported;

    String jwks_uri;

    String authorization_endpoint;

    String registration_endpoint;

    List<String> scopes_supported;

    List<String> response_types_supported;

    List<String> subject_types_supported;

    List<String> id_token_signing_alg_values_supported;

    List<String> claims_supported;

    @JsonProperty("https://purl.imsglobal.org/spec/lti-platform-configuration")
    LTIPlatformConfiguration ltiPlatformConfiguration;

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getToken_endpoint() {
        return token_endpoint;
    }

    public void setToken_endpoint(String token_endpoint) {
        this.token_endpoint = token_endpoint;
    }

    public List<String> getToken_endpoint_auth_methods_supported() {
        return token_endpoint_auth_methods_supported;
    }

    public void setToken_endpoint_auth_methods_supported(List<String> token_endpoint_auth_methods_supported) {
        this.token_endpoint_auth_methods_supported = token_endpoint_auth_methods_supported;
    }

    public List<String> getToken_endpoint_auth_signing_alg_values_supported() {
        return token_endpoint_auth_signing_alg_values_supported;
    }

    public void setToken_endpoint_auth_signing_alg_values_supported(List<String> token_endpoint_auth_signing_alg_values_supported) {
        this.token_endpoint_auth_signing_alg_values_supported = token_endpoint_auth_signing_alg_values_supported;
    }

    public String getJwks_uri() {
        return jwks_uri;
    }

    public void setJwks_uri(String jwks_uri) {
        this.jwks_uri = jwks_uri;
    }

    public String getAuthorization_endpoint() {
        return authorization_endpoint;
    }

    public void setAuthorization_endpoint(String authorization_endpoint) {
        this.authorization_endpoint = authorization_endpoint;
    }

    public String getRegistration_endpoint() {
        return registration_endpoint;
    }

    public void setRegistration_endpoint(String registration_endpoint) {
        this.registration_endpoint = registration_endpoint;
    }

    public List<String> getScopes_supported() {
        return scopes_supported;
    }

    public void setScopes_supported(List<String> scopes_supported) {
        this.scopes_supported = scopes_supported;
    }

    public List<String> getResponse_types_supported() {
        return response_types_supported;
    }

    public void setResponse_types_supported(List<String> response_types_supported) {
        this.response_types_supported = response_types_supported;
    }

    public List<String> getSubject_types_supported() {
        return subject_types_supported;
    }

    public void setSubject_types_supported(List<String> subject_types_supported) {
        this.subject_types_supported = subject_types_supported;
    }

    public List<String> getId_token_signing_alg_values_supported() {
        return id_token_signing_alg_values_supported;
    }

    public void setId_token_signing_alg_values_supported(List<String> id_token_signing_alg_values_supported) {
        this.id_token_signing_alg_values_supported = id_token_signing_alg_values_supported;
    }

    public List<String> getClaims_supported() {
        return claims_supported;
    }

    public void setClaims_supported(List<String> claims_supported) {
        this.claims_supported = claims_supported;
    }

    public void setLtiPlatformConfiguration(LTIPlatformConfiguration ltiPlatformConfiguration) {
        this.ltiPlatformConfiguration = ltiPlatformConfiguration;
    }

    public LTIPlatformConfiguration getLtiPlatformConfiguration() {
        return ltiPlatformConfiguration;
    }

    public static class LTIPlatformConfiguration{
        String product_family_code;
        String version;
        List<Message> messages_supported = new ArrayList<>();

        public static class Message{
            String type;
            List<String> placements;

            public String getType() {
                return type;
            }

            public void setType(String type) {
                this.type = type;
            }

            public List<String> getPlacements() {
                return placements;
            }

            public void setPlacements(List<String> placements) {
                this.placements = placements;
            }
        }

        List<String> variables;

        public String getProduct_family_code() {
            return product_family_code;
        }

        public void setProduct_family_code(String product_family_code) {
            this.product_family_code = product_family_code;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public List<Message> getMessages_supported() {
            return messages_supported;
        }

        public void setMessages_supported(List<Message> messages_supported) {
            this.messages_supported = messages_supported;
        }

        public List<String> getVariables() {
            return variables;
        }

        public void setVariables(List<String> variables) {
            this.variables = variables;
        }
    }

}
