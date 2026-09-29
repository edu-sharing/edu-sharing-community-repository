package org.edu_sharing.restservices;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.alfresco.repo.cache.SimpleCache;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.restservices.shared.Filter;
import org.edu_sharing.restservices.shared.Node;
import org.edu_sharing.restservices.shared.NodeRef;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;

/**
 * Regression and performance-proof tests for {@link NodeDao#convertToRest}.
 * <p>
 * Background: the method used to create a brand new {@code Executors.newFixedThreadPool(...)}
 * on every single call and throw it away right afterwards (see the flame-graph analysis that
 * triggered this change). Group A pins down the functional behaviour so it stays correct across
 * the refactor to a shared executor; Group B ({@link #threadReuse_boundedThreadCount()}) proves
 * the thread-bootstrap defect itself: it fails against the unmodified code (red) and turns green
 * once the shared executor is in place.
 * <p>
 * Threading note: Mockito's {@code mockStatic} only intercepts calls made by the thread that
 * opened it, so it cannot be used for code that {@code convertToRest} executes on its internal
 * worker threads. Tests 1/2 therefore drive the single-threaded "run as system user" branch
 * (fully mockable). Tests covering the multi-threaded branch (3 and 5) instead rely on the fact
 * that {@code AuthenticationUtil}, {@code Context} and {@code NodeServiceInterceptor} are pure,
 * thread-local bookkeeping classes that work correctly for real (unmocked) on any thread, and
 * control {@code NodeDao.getNode(...)}'s outcome via a regular (non-static, thread-safe) stub on
 * {@code repoDao.getId()} instead.
 */
@ExtendWith(MockitoExtension.class)
class NodeDaoConvertToRestTest {

    // Mirrors an explicitly configured "repository.nodeConvert.maxPoolSize" (the shipped
    // reference.conf leaves it unset, defaulting to a CPU-count-based value instead).
    private static final int CONFIGURED_POOL_SIZE = 32;

    private RepositoryDao repoDao;
    private Filter propFilter;

    @BeforeEach
    void setUp() throws Exception {
        repoDao = Mockito.mock(RepositoryDao.class);
        propFilter = new Filter();
        // AuthenticationUtil is normally initialised by Spring (a bean's afterPropertiesSet()
        // sets the static "initialized" flag); without it, setFullyAuthenticatedUser(...) below
        // throws IllegalStateException. Calling it directly avoids needing a Spring context.
        new AuthenticationUtil().afterPropertiesSet();
        // Real (unmocked) thread-local auth state: convertToRest's multi-threaded branch reads
        // AuthenticationUtil.getFullyAuthenticatedUser() on the calling thread and passes it into
        // AuthenticationUtil.runAs(...) on the worker threads, which requires a non-null user.
        AuthenticationUtil.setFullyAuthenticatedUser("user-" + UUID.randomUUID());
        // NodeDao's shared executor lazily reads "repository.nodeConvert.maxPoolSize" from
        // LightbendConfigLoader on first use (there is no Spring context in this test), so a
        // minimal, self-contained config is installed here. The executor itself is created only
        // once per JVM (initialization-on-demand holder), so this only has an effect the first
        // time any test in this run actually triggers that lazy initialization.
        installLightbendConfig(CONFIGURED_POOL_SIZE);
    }

    private static void installLightbendConfig(int maxPoolSize) {
        // NodeConvertExecutorProvider is a JVM-wide singleton in production too (constructed once
        // by Spring); only set it up once here as well instead of replacing it (and abandoning its
        // executor) before every single test method.
        if (NodeConvertExecutorProvider.get() != null) {
            return;
        }
        Config config = ConfigFactory.parseString("repository.nodeConvert.maxPoolSize = " + maxPoolSize);
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

    @AfterEach
    void tearDown() {
        AuthenticationUtil.clearCurrentSecurityContext();
    }

    // ---- Group A: correctness, driven via the single-threaded "system user" branch ----

    @Test
    void convertToRest_returnsAllNodesInOrder() {
        try (MockedStatic<AuthenticationUtil> auth = Mockito.mockStatic(AuthenticationUtil.class);
             MockedStatic<NodeDao> nodeDao = Mockito.mockStatic(NodeDao.class, Mockito.CALLS_REAL_METHODS)) {
            auth.when(AuthenticationUtil::isRunAsUserTheSystemUser).thenReturn(true);

            List<String> ids = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                ids.add(UUID.randomUUID().toString());
            }
            ids.forEach(id -> stubGetNode(nodeDao, id));
            List<NodeRef> refs = ids.stream().map(id -> new NodeRef("home", id)).collect(Collectors.toList());

            List<Node> result = NodeDao.convertToRest(repoDao, refs, propFilter, null);

            assertEquals(ids, result.stream().map(n -> n.getRef().getId()).collect(Collectors.toList()));
        }
    }

    @Test
    void convertToRest_filtersMissingNodes() {
        try (MockedStatic<AuthenticationUtil> auth = Mockito.mockStatic(AuthenticationUtil.class);
             MockedStatic<NodeDao> nodeDao = Mockito.mockStatic(NodeDao.class, Mockito.CALLS_REAL_METHODS)) {
            auth.when(AuthenticationUtil::isRunAsUserTheSystemUser).thenReturn(true);

            String okId = UUID.randomUUID().toString();
            String missingId = UUID.randomUUID().toString();
            stubGetNode(nodeDao, okId);
            nodeDao.when(() -> NodeDao.getNode(eq(repoDao), eq(missingId), eq(propFilter)))
                    .thenThrow(new DAOMissingException(new RuntimeException("missing")));

            List<NodeRef> refs = List.of(new NodeRef("home", okId), new NodeRef("home", missingId));

            List<Node> result = NodeDao.convertToRest(repoDao, refs, propFilter, null);

            assertEquals(1, result.size());
            assertEquals(okId, result.get(0).getRef().getId());
        }
    }

    @Test
    void convertToRest_emptyList_returnsEmptyResult() {
        // No mocking needed: the (real) multi-threaded branch is entered, but with an empty
        // input list no task is ever submitted to the executor.
        List<Node> result = NodeDao.convertToRest(repoDao, List.of(), propFilter, null);
        assertTrue(result.isEmpty());
    }

    // ---- Multi-threaded branch: DAOSecurityException -> dummy fallback, thread reuse ----

    @Test
    void convertToRest_returnsDummyForSecurityException() {
        Mockito.when(repoDao.getId()).thenThrow(new DAOSecurityException(new RuntimeException("denied")));

        List<NodeRef> refs = List.of(new NodeRef("home", "id1"), new NodeRef("home", "id2"));

        List<Node> result = NodeDao.convertToRest(repoDao, refs, propFilter, null);

        assertEquals(2, result.size());
        for (Node dummy : result) {
            assertEquals(CCConstants.getValidLocalName(CCConstants.CCM_TYPE_IO), dummy.getType());
        }
        assertEquals(
                List.of("id1", "id2"),
                result.stream().map(n -> n.getRef().getId()).collect(Collectors.toList())
        );
    }

    // ---- Group B: proves the fix (thread reuse instead of one fresh pool per call) ----

    @Test
    void threadReuse_boundedThreadCount() {
        // Every requested node fails the same (real, non-static-mock-dependent) way, so the
        // executor's exception handling runs 500 times without ever needing a live Alfresco
        // context - we only care about how many OS threads get created along the way.
        Mockito.when(repoDao.getId()).thenThrow(new DAOMissingException(new RuntimeException("n/a")));

        int calls = 50;
        int nodesPerCall = 10;
        // Mirrors "repository.nodeConvert.maxPoolSize" (installed via installLightbendConfig above)
        // without depending on NodeDao's internal executor field, so this test still compiles - and
        // fails - against the unmodified, per-call-pool code.
        long expectedMaxNewThreads = CONFIGURED_POOL_SIZE;

        long startedBefore = ManagementFactory.getThreadMXBean().getTotalStartedThreadCount();

        for (int c = 0; c < calls; c++) {
            List<NodeRef> refs = new ArrayList<>();
            for (int n = 0; n < nodesPerCall; n++) {
                refs.add(new NodeRef("home", UUID.randomUUID().toString()));
            }
            List<Node> result = NodeDao.convertToRest(repoDao, refs, propFilter, null);
            assertEquals(0, result.size());
        }

        long startedAfter = ManagementFactory.getThreadMXBean().getTotalStartedThreadCount();
        long delta = startedAfter - startedBefore;

        assertTrue(delta <= expectedMaxNewThreads,
                "Started " + delta + " new threads for " + calls + " calls (expected <= "
                        + expectedMaxNewThreads + "). This indicates a fresh thread pool is still "
                        + "being created per call instead of a shared, reused executor.");
    }

    private void stubGetNode(MockedStatic<NodeDao> nodeDaoMockedStatic, String id) {
        NodeDao dao = Mockito.mock(NodeDao.class);
        Node node = new Node();
        node.setRef(new NodeRef("home", id));
        Mockito.when(dao.asNode()).thenReturn(node);
        nodeDaoMockedStatic.when(() -> NodeDao.getNode(eq(repoDao), eq(id), eq(propFilter))).thenReturn(dao);
    }
}
