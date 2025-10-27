package org.edu_sharing.spring.security.oauth2.config;

import com.typesafe.config.Optional;
import lombok.Data;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.authentication.sso.mapping.Mapping;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class OAuth2ClientProperties {

    public static final String REGISTRATION_ID_SPLITTER = "_";

    /**
     * Extracts the registration key from the provided registration ID.
     * If the registration ID does not contain the defined splitter,
     * the entire registration ID is returned as the key. Otherwise,
     * the part of the string following the splitter is returned.
     *
     * @param registrationId the complete registration identifier, which may include a prefix and splitter.
     * @return the extracted registration key, or the original registration ID if no splitter is found.
     */
    public static String getRegistrationKey(String registrationId) {
        if(!registrationId.contains(REGISTRATION_ID_SPLITTER)){
           return registrationId;
        }
        return registrationId.substring(registrationId.indexOf(REGISTRATION_ID_SPLITTER) + 1);
    }

    /**
     * Extracts the context ID from the provided registration ID.
     * The context ID is the portion of the registration ID that appears before the defined splitter.
     * If the registration ID does not contain the splitter, null is returned.
     *
     * @param registrationId the complete registration identifier, which may include a context and a splitter
     * @return the extracted context ID, or null if the splitter is not found
     */
    public static String getContextId(String registrationId) {
        if(!registrationId.contains(REGISTRATION_ID_SPLITTER)){
            return CCConstants.EDUCONTEXT_DEFAULT;
        }
        return registrationId.substring(0, registrationId.indexOf(REGISTRATION_ID_SPLITTER));
    }

    /**
     * Constructs a registration ID by combining the context ID and a provided registration key.
     * If the context ID is null, the method simply returns the provided registration key.
     *
     * @param registrationKey the unique key identifying the registration.
     * @return the complete registration ID, combining the context ID and the registration key
     *         separated by a predefined splitter, or the registration key alone if the context ID is null.
     */
    public String getRegistrationId(String registrationKey) {
        if (contextId == null) {
            return registrationKey;
        }

        return contextId + REGISTRATION_ID_SPLITTER + registrationKey;
    }

    private String contextId;
    private Map<String, Registration> registration = new LinkedHashMap<>();
    private Map<String, Provider> provider = new LinkedHashMap<>();
    private Map<String, Mapping> mapping = new LinkedHashMap<>();

    @Data
    public static final class Provider {
        /**
         * Authorization URI for the provider.
         */
        @Optional
        private String authorizationUri;
        /**
         * Token URI for the provider.
         */
        @Optional
        private String tokenUri;
        /**
         * User info URI for the provider.
         */
        @Optional
        private String userInfoUri;
        /**
         * User info authentication method for the provider.
         */
        @Optional
        private String userInfoAuthenticationMethod;
        /**
         * Name of the attribute that will be used to extract the username from the call to 'userInfoUri'.
         */
        @Optional
        private String userNameAttribute;
        /**
         * JWK set URI for the provider.
         */
        @Optional
        private String jwkSetUri;
        /**
         * URI that can either be an OpenID Connect discovery endpoint or an OAuth 2.0 Authorization Server Metadata endpoint defined by RFC 8414.
         */
        @Optional
        private String issuerUri;

    }

    @Data
    public static final class Registration {
        /**
         * Reference to the OAuth 2.0 provider to use. May reference an element from the 'provider' property or used one of the commonly used providers (google, github, facebook, okta).
         */
        @Optional
        private String provider;
        /**
         * Client ID for the registration.
         */
        private String clientId;
        /**
         * CClient secret of the registration.
         */
        private String clientSecret;
        /**
         * Client authentication method. May be left blank when using a pre-defined provider.
         */
        @Optional
        private String clientAuthenticationMethod;
        /**
         * Authorization grant type. May be left blank when using a pre-defined provider.
         */
        @Optional
        private String authorizationGrantType;
        /**
         * Redirect URI. May be left blank when using a pre-defined provider.
         */
        @Optional
        private String redirectUri;
        /**
         * Authorization scope. When left blank the provider's default scope, if any, will be used.
         */
        @Optional
        private List<String> scope;
        /**
         * Client name. May be left blank when using a pre-defined provider
         */
        @Optional
        private String clientName;
    }
}


