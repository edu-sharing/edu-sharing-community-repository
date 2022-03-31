package org.edu_sharing.graphql.util;

import com.typesafe.config.Optional;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.edu_sharing.lightbend.ConfigurationProperties;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Executor;

@Data
@ConfigurationProperties(prefix = "repository.graphql.dataLoaderThreadPool")
public class GraphQLExecutorFactoryProperties {
    public static final Duration DEFAULT_KEEP_ALIVE = Duration.ofSeconds(0);

    @Optional int coreThreadPoolSize = Runtime.getRuntime().availableProcessors();
    @Optional int maxThreadPoolSize = Runtime.getRuntime().availableProcessors();
    @Optional Duration threadKeepAlive = DEFAULT_KEEP_ALIVE;
}