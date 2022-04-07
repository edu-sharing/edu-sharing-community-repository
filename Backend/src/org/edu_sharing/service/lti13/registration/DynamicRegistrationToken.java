package org.edu_sharing.service.lti13.registration;

import java.util.Objects;

public class DynamicRegistrationToken {
    String token;
    String url;
    String registeredAppId;
    long tsCreated;
    long tsExpiry;
    boolean expired=false;

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

    public void setTsExpiry(long l) {tsExpiry = l;}

    public long getTsExpiry() {return tsExpiry;}

    public void setRegisteredAppId(String registeredAppId) {this.registeredAppId = registeredAppId;}

    public String getRegisteredAppId() {return registeredAppId;}

    public void setExpired(boolean expired) {
        this.expired = expired;
    }

    public boolean isExpired() {
        return expired;
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
