package org.edu_sharing.service.statusreport.model.v1;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * payload sent to the central status server, schema version 1
 *
 * Adding optional fields is allowed within this version (the receiver has to ignore unknown fields),
 * breaking changes require a new schema version in a new package (model.v2).
 * The installation is identified by the client id of the basic auth, it is intentionally not part of the payload.
 */
@Data
@JsonInclude(JsonInclude.Include.ALWAYS)
public class StatusReportV1 {
    public static final String SCHEMA_VERSION = "1";

    private String schemaVersion = SCHEMA_VERSION;
    /**
     * iso 8601 timestamp (utc) when the report was created
     */
    private String generatedAt;
    private Installation installation;
    private List<Component> components = new ArrayList<>();
    /**
     * connected systems that can not report a version (lms, cms, remote repositories, ...)
     */
    private Map<String, Integer> connectedSystems = new TreeMap<>();
}
