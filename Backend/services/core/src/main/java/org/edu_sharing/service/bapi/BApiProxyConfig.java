package org.edu_sharing.service.bapi;

import com.typesafe.config.Optional;
import lombok.Data;
import org.edu_sharing.lightbend.ConfigurationProperties;

import java.util.List;

@Data
@ConfigurationProperties( prefix = "repository.bapi")
public class BApiProxyConfig {
   private String uri;
   @Optional
   private String authUserApiKey;
   @Optional
   private String guestUserApiKey;
   @Optional
   private String callTimeout = "PT1m";
   /**
    * What the B-API expects in {@code aud} of the user assertion (the name of the service); set to the same value there
    * ({@code app.edu-sharing.user-header.audience}). Always global.
    */
   @Optional
   private String audience = "bildungs-api";
   @Optional
   private List<String> features = List.of();
   /**
    * If true, api keys not set in a context are inherited from the global repository.bapi config
    */
   @Optional
   private boolean fallback = true;
}
