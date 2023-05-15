package org.edu_sharing.graphql.web.servlet;

import com.typesafe.config.Optional;
import graphql.kickstart.execution.context.ContextSetting;
import lombok.Data;
import org.edu_sharing.lightbend.ConfigurationProperties;

import java.time.Duration;

@Data
@ConfigurationProperties(prefix = "repository.graphql.servlet")
public class GraphQLServletProperties {

    public static final Duration DEFAULT_SUBSCRIPTION_TIMEOUT = Duration.ZERO;


    @Optional boolean exceptionHandlersEnabled = false;
    @Optional Duration subscriptionTimeout = DEFAULT_SUBSCRIPTION_TIMEOUT;
    @Optional ContextSetting contextSetting = ContextSetting.PER_QUERY_WITH_INSTRUMENTATION;
}
