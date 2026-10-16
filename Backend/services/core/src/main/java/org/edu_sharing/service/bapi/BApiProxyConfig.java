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
   @Optional
   private List<String> features = List.of();
   /**
    * If true, api keys not set in a context are inherited from the global repository.bapi config
    */
   @Optional
   private boolean fallback = true;
}
