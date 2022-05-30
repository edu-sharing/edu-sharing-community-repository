package org.edu_sharing.service.rating;

import org.alfresco.service.cmr.repository.NodeRef;

import java.io.Serializable;

public class Rating implements Serializable {
    private double rating;
    private String authority;
    private String text;
    private NodeRef ref;

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public String getAuthority() {
        return authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setRef(NodeRef ref) {
        this.ref = ref;
    }

    public NodeRef getRef() {
        return ref;
    }
}
