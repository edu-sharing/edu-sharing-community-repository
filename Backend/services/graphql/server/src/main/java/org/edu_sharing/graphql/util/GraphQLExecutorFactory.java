package org.edu_sharing.graphql.util;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.Executor;

@Component
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class GraphQLExecutorFactory {

    final GraphQLExecutorFactoryProperties properties;

    public Executor newExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getCoreThreadPoolSize());
        executor.setMaxPoolSize(properties.getMaxThreadPoolSize());
        executor.setKeepAliveSeconds((int)properties.getThreadKeepAlive().getSeconds());
        executor.initialize();

        return new Executor() {
            final Map<String, String> mdcContext = MDC.getCopyOfContextMap();
            final Executor delegate = executor;

            @Override
            public void execute(@NonNull Runnable runnable) {
                delegate.execute(() -> {
                    try {
                        MDC.setContextMap(mdcContext);
                        runnable.run();
                    } finally {
                        MDC.clear();
                    }
                });
            }
        };
    }
}
