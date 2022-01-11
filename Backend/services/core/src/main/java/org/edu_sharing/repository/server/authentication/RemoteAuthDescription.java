package org.edu_sharing.repository.server.authentication;

import java.io.Serializable;

public class RemoteAuthDescription implements Serializable {
    private String url;
    private String token;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
