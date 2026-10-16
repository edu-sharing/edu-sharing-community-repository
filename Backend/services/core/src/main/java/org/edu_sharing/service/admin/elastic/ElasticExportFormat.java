package org.edu_sharing.service.admin.elastic;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ElasticExportFormat {
    /**
     * one row per hit, columns = requested properties
     */
    csv("text/csv", "csv"),
    /**
     * json array, one object per hit with the requested properties
     */
    json("application/json", "json"),
    /**
     * full backup of all matching documents: zip with mapping, settings and the raw _source documents
     * as chunked NDJSON in the elastic _bulk format
     */
    dump("application/zip", "zip");

    private final String mimetype;
    private final String extension;
}
