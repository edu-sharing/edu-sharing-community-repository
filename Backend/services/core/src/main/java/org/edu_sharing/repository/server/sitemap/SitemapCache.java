package org.edu_sharing.repository.server.sitemap;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.function.LongSupplier;

/**
 * The rendered sitemap files, kept for a while.
 *
 * Building a file is expensive: a page reads 5.000 nodes from the index, the sitemap index walks over all of them.
 * A crawler fetches the same files again and again, and between two fetches hardly anything changes. Concurrent
 * requests for the same file build it once, the others wait for that build.
 *
 * Kept in the memory of the repository, per instance: the responses carry the cookie of the guest session, so a
 * shared cache in front (Varnish, nginx) would not keep them anyway, and nothing has to be shared in a cluster.
 */
final class SitemapCache {

    /**
     * @param created epoch millis of the build, the Last-Modified of the response
     */
    record Entry(byte[] body, String etag, long created) {
    }

    private final LongSupplier clock;
    /** least recently used first, guarded by itself */
    private final Map<String, Entry> entries;
    private final Map<String, CompletableFuture<Entry>> building = new ConcurrentHashMap<>();

    SitemapCache(int maxEntries, LongSupplier clock) {
        this.clock = clock;
        this.entries = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Entry> eldest) {
                return size() > maxEntries;
            }
        };
    }

    /**
     * The file of the key, built when there is none younger than the ttl.
     *
     * @param ttlMillis 0 or less: no caching, every call builds
     * @throws Exception what the build threw - also for a call that waited for the build of another one
     */
    Entry get(String key, long ttlMillis, Callable<byte[]> build) throws Exception {
        if (ttlMillis <= 0) {
            return entry(build.call());
        }
        Entry cached;
        synchronized (entries) {
            cached = entries.get(key);
        }
        if (cached != null && clock.getAsLong() - cached.created() < ttlMillis) {
            return cached;
        }
        CompletableFuture<Entry> own = new CompletableFuture<>();
        CompletableFuture<Entry> running = building.putIfAbsent(key, own);
        if (running != null) {
            try {
                return running.get();
            } catch (ExecutionException e) {
                throw e.getCause() instanceof Exception cause ? cause : e;
            }
        }
        try {
            Entry entry = entry(build.call());
            synchronized (entries) {
                entries.put(key, entry);
            }
            own.complete(entry);
            return entry;
        } catch (Exception | Error e) {
            // a failed build is not kept, the next request tries again
            own.completeExceptionally(e);
            throw e;
        } finally {
            building.remove(key, own);
        }
    }

    int size() {
        synchronized (entries) {
            return entries.size();
        }
    }

    private Entry entry(byte[] body) {
        return new Entry(body, etag(body), clock.getAsLong());
    }

    /** from the content: an unchanged file keeps its etag after the next build */
    static String etag(byte[] body) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(body);
            return "\"" + Base64.getUrlEncoder().withoutPadding().encodeToString(hash).substring(0, 27) + "\"";
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Whether the client has the file already: If-None-Match wins (as in RFC 9110), else If-Modified-Since.
     *
     * @param ifNoneMatch    the header, null if not sent
     * @param ifModifiedSince the header as epoch millis, -1 if not sent
     */
    static boolean notModified(String ifNoneMatch, long ifModifiedSince, Entry entry) {
        if (ifNoneMatch != null) {
            for (String candidate : ifNoneMatch.split(",")) {
                String tag = candidate.trim();
                if (tag.startsWith("W/")) {
                    tag = tag.substring(2);
                }
                if (tag.equals("*") || tag.equals(entry.etag())) {
                    return true;
                }
            }
            return false;
        }
        // the header has whole seconds
        return ifModifiedSince >= 0 && entry.created() / 1000 * 1000 <= ifModifiedSince;
    }
}
