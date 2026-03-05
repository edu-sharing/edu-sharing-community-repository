package org.edu_sharing.service.bapi;

import com.typesafe.config.Optional;
import lombok.Data;
import org.edu_sharing.lightbend.ConfigurationProperties;

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
}