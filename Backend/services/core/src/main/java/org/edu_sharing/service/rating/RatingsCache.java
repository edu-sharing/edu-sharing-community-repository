package org.edu_sharing.service.rating;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.HashMap;

@Schema
public class RatingsCache extends RatingBase implements Serializable {
    // ratings per user/authority
    private HashMap<String, Double> users;


    public <R> void setUsers(HashMap<String, Double> users) {
        this.users = users;
    }

    public HashMap<String, Double> getUsers() {
        return users;
    }
}
