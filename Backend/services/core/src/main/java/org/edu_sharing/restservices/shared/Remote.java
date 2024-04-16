package org.edu_sharing.restservices.shared;

import org.codehaus.jackson.annotate.JsonProperty;

import java.io.Serializable;

public class Remote implements Serializable {
    @JsonProperty
    private Repo repository;
    @JsonProperty
    private String id;

    public Repo getRepository() {
        return repository;
    }

    public void setRepository(Repo repository) {
        this.repository = repository;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
