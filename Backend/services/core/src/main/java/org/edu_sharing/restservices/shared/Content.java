package org.edu_sharing.restservices.shared;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Content {
    @JsonProperty
    private String url;
    @JsonProperty
    private String hash;
    @JsonProperty
    private String version;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
