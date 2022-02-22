package org.edu_sharing.service.lti13.registration;

import java.util.Objects;

public class DynamicRegistrationToken {
    String token;
    String url;
    long tsCreated;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public long getTsCreated() {
        return tsCreated;
    }

    public void setTsCreated(long tsCreated) {
        this.tsCreated = tsCreated;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DynamicRegistrationToken that = (DynamicRegistrationToken) o;
        return token.equals(that.token);
    }

    @Override
    public int hashCode() {
        return Objects.hash(token);
    }
}
