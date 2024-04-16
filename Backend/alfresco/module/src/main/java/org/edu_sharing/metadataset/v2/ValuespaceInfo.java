package org.edu_sharing.metadataset.v2;

import java.io.Serializable;

public class ValuespaceInfo implements Serializable {
    public enum ValuespaceType {
        SKOS
    }
    private String value;
    private ValuespaceType type;


    public ValuespaceInfo(String value, ValuespaceType type) {
        this.value = value;
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public ValuespaceType getType() {
        return type;
    }

    public void setType(ValuespaceType type) {
        this.type = type;
    }
}
