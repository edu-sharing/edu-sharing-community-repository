package org.edu_sharing.service.search.model;

import lombok.Value;

/**
 * Lightweight view of an index document for the sitemap, so no further repository lookups are needed.
 */
@Value
public class SitemapEntry {
    String nodeId;
    /**
     * the position in the sitemap order, used as the "after" cursor of the next page
     */
    long dbid;
    /**
     * epoch millis of cm:modified, null if unknown
     */
    Long modified;
    boolean collection;
    String mimetype;
    String name;
}
