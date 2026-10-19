package org.edu_sharing.service.search.model;

import lombok.Value;

/**
 * One page of a sitemap, identified by the cursor it starts after.
 */
@Value
public class SitemapPage {
    /**
     * dbid of the last entry of the previous page, null for the first page
     */
    Long after;
    /**
     * epoch millis of the newest cm:modified on this page, null if unknown
     */
    Long lastModified;
}
