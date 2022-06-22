package org.edu_sharing.graphql.web.servlet;

import com.typesafe.config.Optional;
import lombok.Data;
import org.edu_sharing.lightbend.ConfigurationProperties;

import java.time.Duration;

@Data
@ConfigurationProperties(prefix = "repository.graphql.servlet.async")
public class AsyncServletProperties {
    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    @Optional boolean enabled = true;
    @Optional Duration timeout = DEFAULT_TIMEOUT;
    @Optional boolean delegateSecurityContext = true;
    @Optional Threads threads = new Threads();

    @Data
    public static class Threads {
        @Optional int min = 10;
        @Optional int max = 200;
        @Optional String namePrefix = "graphql-exec-";
    }
}
