package org.edu_sharing.restservices;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.alfresco.repo.cache.SimpleCache;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.restservices.shared.Filter;
import org.edu_sharing.restservices.shared.NodeRef;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Manual before/after benchmark for the convertToRest() thread-pool-per-call fix. Deliberately
 * NOT named "*Test" so Surefire's default include pattern skips it in the regular build; run it
 * explicitly:
 * <pre>
 *   mvn -pl Backend/services/core test -Dtest=NodeDaoConvertToRestBenchmark
 * </pre>
 * Every requested node fails the same (real, non-static-mock-dependent) way - see
 * {@link NodeDaoConvertToRestTest} for why that is safe and sufficient here; we only care about
 * OS thread creation and wall-clock time, not about the returned data.
 */
class NodeDaoConvertToRestBenchmark {

    @Test
    void measureThreadCreationAndWallClock() throws Exception {
        new AuthenticationUtil().afterPropertiesSet();
        AuthenticationUtil.setFullyAuthenticatedUser("bench-user-" + UUID.randomUUID());
        // NodeDao's shared executor lazily reads "repository.nodeConvert.maxPoolSize" from
        // LightbendConfigLoader on first use; there is no Spring context here, so a minimal,
        // self-contained config is installed (an empty config leaves the path unset, exercising
        // the same CPU-count-based default the shipped reference.conf falls back to).
        installLightbendConfig(null);
        try {
            RepositoryDao repoDao = Mockito.mock(RepositoryDao.class);
            Mockito.when(repoDao.getId()).thenThrow(new DAOMissingException(new RuntimeException("n/a")));
            Filter propFilter = new Filter();

            int calls = 2000;
            int nodesPerCall = 20;

            long startedBefore = ManagementFactory.getThreadMXBean().getTotalStartedThreadCount();
            long wallStartNanos = System.nanoTime();

            for (int c = 0; c < calls; c++) {
                List<NodeRef> refs = new ArrayList<>();
                for (int n = 0; n < nodesPerCall; n++) {
                    refs.add(new NodeRef("home", UUID.randomUUID().toString()));
                }
                NodeDao.convertToRest(repoDao, refs, propFilter, null);
            }

            long wallNanos = System.nanoTime() - wallStartNanos;
            long startedAfter = ManagementFactory.getThreadMXBean().getTotalStartedThreadCount();

            System.out.println("=== convertToRest benchmark ===");
            System.out.println("calls=" + calls + " nodesPerCall=" + nodesPerCall);
            System.out.println("threadsStarted=" + (startedAfter - startedBefore));
            System.out.println("wallMillis=" + (wallNanos / 1_000_000));
        } finally {
            AuthenticationUtil.clearCurrentSecurityContext();
        }
    }

    private static void installLightbendConfig(Integer maxPoolSize) {
        if (NodeConvertExecutorProvider.get() != null) {
            return;
        }
        Config config = maxPoolSize == null
                ? ConfigFactory.empty()
                : ConfigFactory.parseString("repository.nodeConvert.maxPoolSize = " + maxPoolSize);
        LightbendConfigLoader lightbendConfigLoader = new LightbendConfigLoader(new SimpleCache<String, Config>() {
            @Override
            public boolean contains(String key) {
                return "config".equals(key);
            }

            @Override
            public Collection<String> getKeys() {
                return List.of("config");
            }

            @Override
            public Config get(String key) {
                return config;
            }

            @Override
            public void put(String key, Config value) {
            }

            @Override
            public void remove(String key) {
            }

            @Override
            public void clear() {
            }
        });
        new NodeConvertExecutorProvider(lightbendConfigLoader);
    }
}
