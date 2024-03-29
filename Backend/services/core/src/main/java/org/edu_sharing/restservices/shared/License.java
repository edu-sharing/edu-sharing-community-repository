package org.edu_sharing.restservices.shared;

import org.codehaus.jackson.annotate.JsonProperty;

import java.io.Serializable;

public class License implements Serializable {
    @JsonProperty String icon;
    @JsonProperty String url;

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
