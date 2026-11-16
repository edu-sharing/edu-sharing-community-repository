package org.edu_sharing.repository.server.sitemap;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SitemapCacheTest {

    private final AtomicLong now = new AtomicLong(1_000_000);
    private final AtomicInteger builds = new AtomicInteger();

    private byte[] build(String content) {
        builds.incrementAndGet();
        return content.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void keptForTheTtl() throws Exception {
        SitemapCache cache = new SitemapCache(10, now::get);
        SitemapCache.Entry first = cache.get("a", 600_000, () -> build("x"));
        now.addAndGet(599_999);
        assertSame(first, cache.get("a", 600_000, () -> build("y")));
        assertEquals(1, builds.get());
    }

    @Test
    void rebuiltAfterTheTtl() throws Exception {
        SitemapCache cache = new SitemapCache(10, now::get);
        cache.get("a", 600_000, () -> build("x"));
        now.addAndGet(600_000);
        SitemapCache.Entry second = cache.get("a", 600_000, () -> build("y"));
        assertArrayEquals("y".getBytes(StandardCharsets.UTF_8), second.body());
        assertEquals(now.get(), second.created());
        assertEquals(2, builds.get());
    }

    @Test
    void ttlZeroBuildsEveryTime() throws Exception {
        SitemapCache cache = new SitemapCache(10, now::get);
        cache.get("a", 0, () -> build("x"));
        cache.get("a", 0, () -> build("x"));
        assertEquals(2, builds.get());
        assertEquals(0, cache.size());
    }

    @Test
    void keysAreSeparate() throws Exception {
        SitemapCache cache = new SitemapCache(10, now::get);
        cache.get("guest", 600_000, () -> build("x"));
        cache.get("admin", 600_000, () -> build("y"));
        assertEquals(2, builds.get());
    }

    @Test
    void theLeastRecentlyUsedFileGoesFirst() throws Exception {
        SitemapCache cache = new SitemapCache(2, now::get);
        cache.get("a", 600_000, () -> build("a"));
        cache.get("b", 600_000, () -> build("b"));
        // used, so b is the least recently used one
        cache.get("a", 600_000, () -> build("a"));
        cache.get("c", 600_000, () -> build("c"));
        assertEquals(2, cache.size());
        cache.get("a", 600_000, () -> build("a"));
        assertEquals(3, builds.get());
        cache.get("b", 600_000, () -> build("b"));
        assertEquals(4, builds.get());
    }

    @Test
    void aFailedBuildIsNotKept() throws Exception {
        SitemapCache cache = new SitemapCache(10, now::get);
        NumberFormatException failure = new NumberFormatException("after=x");
        assertSame(failure, assertThrows(NumberFormatException.class, () -> cache.get("a", 600_000, () -> {
            throw failure;
        })));
        assertEquals(0, cache.size());
        cache.get("a", 600_000, () -> build("x"));
        assertEquals(1, builds.get());
    }

    @Test
    void concurrentRequestsBuildOnce() throws Exception {
        SitemapCache cache = new SitemapCache(10, now::get);
        CountDownLatch building = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<SitemapCache.Entry> first = executor.submit(() -> cache.get("a", 600_000, () -> {
                building.countDown();
                assertTrue(release.await(5, TimeUnit.SECONDS));
                return build("x");
            }));
            assertTrue(building.await(5, TimeUnit.SECONDS));
            Future<SitemapCache.Entry> second = executor.submit(() -> cache.get("a", 600_000, () -> build("y")));
            // the second one waits for the first build instead of starting its own
            Thread.sleep(100);
            assertFalse(second.isDone());
            release.countDown();
            assertSame(first.get(5, TimeUnit.SECONDS), second.get(5, TimeUnit.SECONDS));
            assertEquals(1, builds.get());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void etagFromTheContent() {
        byte[] content = "x".getBytes(StandardCharsets.UTF_8);
        assertEquals(SitemapCache.etag(content), SitemapCache.etag("x".getBytes(StandardCharsets.UTF_8)));
        assertNotEquals(SitemapCache.etag(content), SitemapCache.etag("y".getBytes(StandardCharsets.UTF_8)));
        assertTrue(SitemapCache.etag(content).matches("\"[A-Za-z0-9_-]{27}\""));
    }

    @Test
    void notModifiedByEtag() {
        SitemapCache.Entry entry = new SitemapCache.Entry(new byte[0], "\"abc\"", 1_000_000);
        assertTrue(SitemapCache.notModified("\"abc\"", -1, entry));
        assertTrue(SitemapCache.notModified("\"x\", W/\"abc\"", -1, entry));
        assertTrue(SitemapCache.notModified("*", -1, entry));
        assertFalse(SitemapCache.notModified("\"x\"", -1, entry));
        // If-None-Match wins over If-Modified-Since
        assertFalse(SitemapCache.notModified("\"x\"", 2_000_000, entry));
    }

    @Test
    void notModifiedByDate() {
        SitemapCache.Entry entry = new SitemapCache.Entry(new byte[0], "\"abc\"", 1_000_500);
        // the header has whole seconds
        assertTrue(SitemapCache.notModified(null, 1_000_000, entry));
        assertTrue(SitemapCache.notModified(null, 2_000_000, entry));
        assertFalse(SitemapCache.notModified(null, 999_000, entry));
        assertFalse(SitemapCache.notModified(null, -1, entry));
    }
}
