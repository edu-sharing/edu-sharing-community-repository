package org.edu_sharing.service.search;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SearchServiceElasticSitemapTest {

    @Test
    void linkToAVideoIsAVideo() {
        // like a sodix node: a link to the video file, without its own binary content
        Map<String, Object> source = Map.of(
                "type", "ccm:io",
                "aspects", List.of("ccm:ccm_lifecycle"),
                "properties", Map.of(
                        "cclom:format", List.of("video/mp4"),
                        "ccm:wwwurl", List.of("https://example.org/video.mp4"),
                        "cm:name", List.of("Video.mp4")));
        assertEquals("file-video", SearchServiceElastic.getSitemapMediatype(source));
    }

    @Test
    void fileWithoutFormatIsDetectedByItsExtension() {
        Map<String, Object> source = Map.of(
                "type", "ccm:io",
                "properties", Map.of("cm:name", "clip.mp4"));
        assertEquals("file-video", SearchServiceElastic.getSitemapMediatype(source));
    }

    @Test
    void pdfIsAPdf() {
        Map<String, Object> source = Map.of(
                "type", "ccm:io",
                "properties", Map.of("cclom:format", "application/pdf", "cm:name", "a.pdf"));
        assertEquals("file-pdf", SearchServiceElastic.getSitemapMediatype(source));
    }

    @Test
    void collectionIsACollection() {
        Map<String, Object> source = Map.of(
                "type", "ccm:map",
                "aspects", List.of("ccm:collection"),
                "properties", Map.of("cm:name", "Sammlung"));
        assertEquals("collection", SearchServiceElastic.getSitemapMediatype(source));
    }

    @Test
    void linkWithoutFormatIsALink() {
        Map<String, Object> source = Map.of(
                "type", "ccm:io",
                "properties", Map.of("ccm:wwwurl", "https://example.org/", "cm:name", "Webseite"));
        assertEquals("link", SearchServiceElastic.getSitemapMediatype(source));
    }
}
