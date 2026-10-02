package org.edu_sharing.repository.server.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reproduces race conditions in {@link ApplicationInfoList}: concurrent readers must never observe a missing
 * home repository, a missing rendering service 2 or an incomplete application list - neither on a cold start
 * nor while {@link ApplicationInfoList#refresh()} runs.
 * <p>
 * The application files are real XML files from src/test/resources/config/cluster/applications, because
 * Mockito's mockConstruction is thread-local and would not affect the worker threads.
 * <p>
 * Takes several seconds, so it is skipped by default. Run it with:
 * {@code mvn -pl Backend/alfresco/module test -Dtest=ApplicationInfoListConcurrencyTest -DrunConcurrencyTests=true -Dsurefire.failIfNoSpecifiedTests=false}
 */
@EnabledIfSystemProperty(named = "runConcurrencyTests", matches = "true")
class ApplicationInfoListConcurrencyTest {

    private static final int EXPECTED_APP_COUNT = 5;
    private static final int EXPECTED_LTI_TOOL_COUNT = 1;

    private static final int COLD_START_ROUNDS = 200;
    private static final int COLD_START_READERS = 16;
    private static final int COLD_START_ITERATIONS_PER_READER = 200;

    private static final int REFRESH_READERS = 8;
    private static final long REFRESH_DURATION_MS = 2000;

    private final Map<String, LongAdder> anomalies = new ConcurrentHashMap<>();

    @BeforeEach
    void resetState() throws Exception {
        resetToColdStart();
    }

    @Test
    void coldStart_concurrentReadersNeverSeeNullOrPartialState() throws Exception {
        for (int round = 0; round < COLD_START_ROUNDS; round++) {
            resetToColdStart();
            CountDownLatch start = new CountDownLatch(1);
            List<Thread> readers = new ArrayList<>();
            for (int i = 0; i < COLD_START_READERS; i++) {
                Thread reader = new Thread(() -> {
                    awaitQuietly(start);
                    for (int n = 0; n < COLD_START_ITERATIONS_PER_READER; n++) {
                        readAndCheck();
                    }
                }, "cold-start-reader-" + i);
                readers.add(reader);
                reader.start();
            }
            start.countDown();
            for (Thread reader : readers) {
                reader.join(TimeUnit.SECONDS.toMillis(30));
            }
        }

        assertNoAnomalies("cold start (" + COLD_START_ROUNDS + " rounds x " + COLD_START_READERS + " readers)");
    }

    @Test
    void refreshWhileReading_readersNeverSeeNullOrPartialState() throws Exception {
        // warm up, so readers start from a fully initialized state
        ApplicationInfoList.getApplicationInfos();

        AtomicBoolean running = new AtomicBoolean(true);
        CountDownLatch start = new CountDownLatch(1);
        List<Thread> threads = new ArrayList<>();
        LongAdder refreshCount = new LongAdder();

        threads.add(new Thread(() -> {
            awaitQuietly(start);
            while (running.get()) {
                ApplicationInfoList.refresh();
                refreshCount.increment();
            }
        }, "refresher"));
        for (int i = 0; i < REFRESH_READERS; i++) {
            threads.add(new Thread(() -> {
                awaitQuietly(start);
                while (running.get()) {
                    readAndCheck();
                }
            }, "refresh-reader-" + i));
        }
        threads.forEach(Thread::start);
        start.countDown();

        long deadline = System.currentTimeMillis() + REFRESH_DURATION_MS;
        while (System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }
        running.set(false);
        for (Thread thread : threads) {
            thread.join(TimeUnit.SECONDS.toMillis(30));
        }

        assertNoAnomalies("refresh while reading (" + refreshCount.sum() + " refreshes, " + REFRESH_READERS + " readers)");
    }

    /**
     * one read cycle as a typical caller would do it; every deviation from the expected state is counted
     */
    private void readAndCheck() {
        try {
            if (ApplicationInfoList.getHomeRepository() == null) {
                anomaly("getHomeRepository() == null");
            }
            if (ApplicationInfoList.getRenderingService2() == null) {
                anomaly("getRenderingService2() == null");
            }
            Map<String, ApplicationInfo> appInfos = ApplicationInfoList.getApplicationInfos();
            if (appInfos.size() != EXPECTED_APP_COUNT) {
                anomaly("getApplicationInfos().size() == " + appInfos.size());
            }
            if (appInfos.containsValue(null)) {
                anomaly("getApplicationInfos() contains null values");
            }
            // other2 is the last entry of the registry, so it is missing first in a partially filled state
            if (ApplicationInfoList.getRepositoryInfoById("other2") == null) {
                anomaly("getRepositoryInfoById(\"other2\") == null");
            }
            int ltiCount = 0;
            for (ApplicationInfo ignored : ApplicationInfoList.getAppInfosLtiTool()) {
                ltiCount++;
            }
            if (ltiCount != EXPECTED_LTI_TOOL_COUNT) {
                anomaly("getAppInfosLtiTool().size() == " + ltiCount);
            }
        } catch (ConcurrentModificationException e) {
            anomaly("ConcurrentModificationException");
        } catch (Throwable t) {
            anomaly("exception: " + t.getClass().getName());
        }
    }

    private void anomaly(String key) {
        anomalies.computeIfAbsent(key, k -> new LongAdder()).increment();
    }

    private void assertNoAnomalies(String scenario) {
        Map<String, Long> report = new TreeMap<>();
        anomalies.forEach((k, v) -> report.put(k, v.sum()));
        StringBuilder message = new StringBuilder("race conditions detected in scenario " + scenario + ":");
        report.forEach((k, v) -> message.append("\n  ").append(k).append(" -> ").append(v).append("x"));
        assertTrue(report.isEmpty(), message.toString());
    }

    /**
     * drops the published snapshot, as after a fresh start of the repository
     */
    private static void resetToColdStart() throws Exception {
        Field field = ApplicationInfoList.class.getDeclaredField("snapshot");
        field.setAccessible(true);
        field.set(null, null);
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
