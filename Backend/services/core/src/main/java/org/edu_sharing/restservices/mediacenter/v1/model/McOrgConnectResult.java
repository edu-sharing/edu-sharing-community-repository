package org.edu_sharing.restservices.mediacenter.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class McOrgConnectResult {
    @JsonProperty
    private int rows;

    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        this.rows = rows;
    }
}
