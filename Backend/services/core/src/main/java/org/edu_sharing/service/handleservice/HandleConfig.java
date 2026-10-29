package org.edu_sharing.service.handleservice;

import com.typesafe.config.Optional;
import lombok.Data;
import org.edu_sharing.lightbend.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "repository.handleservice")
public class HandleConfig {
    public enum Mode {
        /**
         * native handle protocol via handle-client library (HandleServiceImpl)
         */
        protocol,
        /**
         * handle.net HTTP JSON REST API (HandleServiceRest)
         */
        rest,
        /**
         * ePIC API (HandleServiceEpic)
         */
        epic
    }

    public enum Auth {
        /**
         * challenge-response with the private key (HS_PUBKEY)
         */
        pubkey,
        /**
         * http basic auth with the secret key (HS_SECKEY)
         */
        basic
    }

    private boolean enabled;
    @Optional
    private Mode mode;
    @Optional
    private String prefix;
    @Optional
    private String privkey;
    @Optional
    private String repoid;
    @Optional
    private String email;
    @Optional
    private String configDir;
    @Optional
    private String adminHandle;
    @Optional
    private Integer adminIndex;
    @Optional
    private Rest rest;
    @Optional
    private Epic epic;

    /**
     * settings for mode rest
     */
    @Data
    public static class Rest {
        @Optional
        private String url;
        @Optional
        private Auth auth;
        @Optional
        private String secretKey;
        @Optional
        private Boolean trustAllCertificates;
    }

    /**
     * settings for mode epic
     */
    @Data
    public static class Epic {
        @Optional
        private String url;
        @Optional
        private String username;
        @Optional
        private String password;
        @Optional
        private Boolean trustAllCertificates;
    }
}
