package org.edu_sharing.service.rating;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class RatingDetails extends RatingBase implements Serializable {
    @JsonProperty
    private double user;

    public double isUser() {
        return user;
    }

    public void setUser(double user) {
        this.user = user;
    }
}
