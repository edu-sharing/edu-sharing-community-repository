package org.edu_sharing.restservices;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.edu_sharing.restservices.shared.Person;
import org.edu_sharing.restservices.shared.User;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

public class CollectionFeedback implements Serializable {
    @JsonProperty private Date createdAt;
    @JsonProperty private Person createdBy;
    @JsonProperty private Map<String,Serializable> feedback;

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Person getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Person createdBy) {
        this.createdBy = createdBy;
    }

    public Map<String, Serializable> getFeedback() {
        return feedback;
    }

    public void setFeedback(Map<String, Serializable> feedback) {
        this.feedback = feedback;
    }
}
