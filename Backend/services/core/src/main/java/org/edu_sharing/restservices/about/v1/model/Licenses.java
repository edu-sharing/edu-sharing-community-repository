package org.edu_sharing.restservices.about.v1.model;

import org.codehaus.jackson.annotate.JsonProperty;

import java.util.HashMap;
import java.util.Map;

public class Licenses {
    @JsonProperty
    private final Map<String, String> repository = new HashMap<>();
    private final Map<Services, Map<String, String>> services = new HashMap<>();


    public Map<Services, Map<String, String>> getServices() {
        return services;
    }

    public Map<String, String> getRepository() {
        return repository;
    }
}
