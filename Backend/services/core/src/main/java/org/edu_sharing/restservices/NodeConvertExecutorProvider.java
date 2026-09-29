package org.edu_sharing.restservices;

import com.typesafe.config.Config;
import jakarta.annotation.PreDestroy;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.spring.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Provides the shared, long-lived executor used by
 * {@link NodeDao#convertToRest(RepositoryDao, java.util.List, org.edu_sharing.restservices.shared.Filter, java.util.function.Function)}
 * to fetch/convert multiple nodes in parallel. Previously a brand new
 * {@code Executors.newFixedThreadPool(...)} was created (and immediately shut down again) on every
 * single call, which a flame-graph analysis showed to be the single biggest CPU cost in
 * listing/search responses (~20% of CPU time, ~12-15% of all samples spent purely in OS thread
 * bootstrap). Reusing a shared pool eliminates that overhead.
 * <p>
 * Sized elastically (core/max configurable via {@code repository.nodeConvert.corePoolSize} /
 * {@code .maxPoolSize}, defaulting to CPU count / 4x CPU count when unset) rather than as a small
 * fixed pool: this executor is shared by every concurrent REST request that lists/searches nodes,
 * so a small fixed size would turn into a request bottleneck under load. A {@link SynchronousQueue}
 * makes the pool grow immediately (instead of queuing) up to the max size, and
 * {@link ThreadPoolExecutor.CallerRunsPolicy} provides graceful backpressure (the calling request
 * thread runs the task itself) once that max is reached, instead of rejecting work or growing
 * unbounded. Idle threads beyond keep-alive time are reclaimed.
 * <p>
 * Registered as a plain singleton bean rather than {@code @RefreshScope}: {@link NodeDao} is a
 * purely static utility class with no Spring wiring, so it cannot go through a scoped proxy on
 * every call - it instead holds a static reference to this bean ({@link #get()}) and calls
 * {@link #getExecutor()} on every invocation. Reacting to {@link RefreshScopeRefreshedEvent} by
 * tearing down the current executor achieves the same "rebuild the pool after a config reload"
 * outcome as {@code @RefreshScope} while remaining compatible with that static access pattern - the
 * next {@link #getExecutor()} call after a refresh lazily rebuilds the pool with the current config.
 */
@Component
public class NodeConvertExecutorProvider {

    private static NodeConvertExecutorProvider instance;

    private final LightbendConfigLoader lightbendConfigLoader;
    private volatile ExecutorService executor;

    public NodeConvertExecutorProvider(LightbendConfigLoader lightbendConfigLoader) {
        this.lightbendConfigLoader = lightbendConfigLoader;
        instance = this;
    }

    public static NodeConvertExecutorProvider get() {
        return instance;
    }

    public ExecutorService getExecutor() {
        ExecutorService result = executor;
        if (result == null) {
            synchronized (this) {
                result = executor;
                if (result == null) {
                    executor = result = createExecutor();
                }
            }
        }
        return result;
    }

    @EventListener
    public synchronized void onConfigurationChanged(RefreshScopeRefreshedEvent event) {
        if (executor != null) {
            executor.shutdown();
            executor = null;
        }
    }

    @PreDestroy
    public synchronized void shutdown() {
        if (executor != null) {
            executor.shutdown();
            executor = null;
        }
    }

    private ExecutorService createExecutor() {
        Config config = lightbendConfigLoader.getConfig();
        int defaultCorePoolSize = Runtime.getRuntime().availableProcessors();
        int defaultMaxPoolSize = Math.max(32, defaultCorePoolSize * 4);
        int corePoolSize = config.hasPath("repository.nodeConvert.corePoolSize")
                ? config.getInt("repository.nodeConvert.corePoolSize")
                : defaultCorePoolSize;
        int configuredMaxPoolSize = config.hasPath("repository.nodeConvert.maxPoolSize")
                ? config.getInt("repository.nodeConvert.maxPoolSize")
                : defaultMaxPoolSize;
        // ThreadPoolExecutor requires corePoolSize <= maximumPoolSize; if configured (or defaulted)
        // the other way round, clamp up rather than let the constructor throw.
        int maxPoolSize = Math.max(corePoolSize, configuredMaxPoolSize);
        ThreadFactory threadFactory = new ThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger();

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "NodeDao-convertToRest-" + counter.incrementAndGet());
                thread.setDaemon(true);
                return thread;
            }
        };
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                60L, TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                threadFactory,
                new ThreadPoolExecutor.CallerRunsPolicy());
        threadPoolExecutor.allowCoreThreadTimeOut(true);
        return threadPoolExecutor;
    }
}
