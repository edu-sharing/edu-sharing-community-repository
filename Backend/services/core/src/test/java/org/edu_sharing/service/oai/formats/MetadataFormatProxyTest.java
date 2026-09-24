package org.edu_sharing.service.oai.formats;

import io.gdcc.xoai.dataprovider.model.MetadataFormat;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression test for the intermittent OAI 500 under load
 * ({@code WstxEOFException: Unexpected EOF in prolog}) caused by a shared, non-thread-safe
 * {@link Transformer}.
 *
 * <p>Each format writer used to cache a single {@link Transformer} (via
 * {@code MetadataFormat.withTransformer(MetadataFormat.identity())}) in a Spring singleton. Both
 * {@link AbstractMetadataFormatWriter#write} and XOAI's own {@code MetadataHelper.process} then
 * called {@code transform()} on that shared instance concurrently, corrupting output and producing
 * empty metadata strings. {@link MetadataFormatProxy} obtains a fresh transformer from its
 * injected factory per call to remove the shared mutable state.</p>
 */
class MetadataFormatProxyTest {

    private static MetadataFormat lomFormat() {
        return new MetadataFormatProxy(MetadataFormat::identity)
                .withPrefix("lom")
                .withNamespace("http://ltsc.ieee.org/xsd/LOM");
    }

    /** The format still exposes its configured metadata (prefix/namespace). */
    @Test
    void exposesConfiguredMetadata() {
        MetadataFormat format = lomFormat();
        assertEquals("lom", format.getPrefix());
        assertEquals("http://ltsc.ieee.org/xsd/LOM", format.getNamespace());
    }

    /** The fix contract: never hand out the same transformer twice (fails on the old cached field). */
    @Test
    void getTransformerReturnsFreshInstanceEachCall() {
        MetadataFormat format = lomFormat();
        Transformer first = format.getTransformer();
        Transformer second = format.getTransformer();
        assertNotSame(first, second, "getTransformer() must not share a single Transformer instance");
    }

    /**
     * Mirrors {@link AbstractMetadataFormatWriter#write} (build DOM, then
     * {@code getFormat().getTransformer().transform(...)}) from many threads and asserts every
     * serialization produces well-formed, non-empty XML. With a shared transformer this races and
     * yields blank/prolog-only output; with {@link MetadataFormatProxy} it stays stable.
     */
    @Test
    void concurrentSerializationNeverProducesEmptyOutput() throws Exception {
        MetadataFormat format = lomFormat();

        int threads = 16;
        int iterationsPerThread = 200;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        List<String> failures = new CopyOnWriteArrayList<>();

        for (int t = 0; t < threads; t++) {
            pool.submit(() -> {
                try {
                    start.await();
                    for (int i = 0; i < iterationsPerThread; i++) {
                        String xml = serializeSampleDocument(format);
                        if (xml == null || xml.isBlank()) {
                            failures.add("blank output");
                            continue;
                        }
                        // must be parseable and carry the expected root element
                        DocumentBuilderFactory.newInstance().newDocumentBuilder()
                                .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
                        if (!xml.contains("<lom")) {
                            failures.add("missing root element: " + xml);
                            continue;
                        }
                        success.incrementAndGet();
                    }
                } catch (Exception e) {
                    failures.add(e.getClass().getSimpleName() + ": " + e.getMessage());
                }
            });
        }

        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(60, TimeUnit.SECONDS), "workers did not finish in time");

        assertTrue(failures.isEmpty(), "concurrent serialization produced failures: " + failures);
        assertEquals(threads * iterationsPerThread, success.get());
    }

    /** Replicates the DOM-to-stream serialization done by {@link AbstractMetadataFormatWriter#write}. */
    private static String serializeSampleDocument(MetadataFormat format) throws Exception {
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Element root = document.createElement("lom");
        document.appendChild(root);
        Element title = document.createElement("title");
        title.setTextContent("Sample title for concurrency check");
        root.appendChild(title);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        Transformer transformer = format.getTransformer();
        transformer.transform(new DOMSource(document), new StreamResult(os));
        return os.toString(StandardCharsets.UTF_8);
    }
}
