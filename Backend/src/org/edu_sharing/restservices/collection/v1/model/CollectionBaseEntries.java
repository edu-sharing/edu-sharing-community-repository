package org.edu_sharing.restservices.collection.v1.model;

import org.edu_sharing.restservices.shared.Pagination;

import java.util.ArrayList;
import java.util.List;

public class CollectionBaseEntries {

    private List<CollectionBase> entries = new ArrayList<>();
    private Pagination pagination;

    public List<CollectionBase> getEntries() {
        return entries;
    }

    public void setEntries(List<CollectionBase> entries) {
        this.entries = entries;
    }

    public Pagination getPagination() {
        return pagination;
    }

    public void setPagination(Pagination pagination) {
        this.pagination = pagination;
    }
}
