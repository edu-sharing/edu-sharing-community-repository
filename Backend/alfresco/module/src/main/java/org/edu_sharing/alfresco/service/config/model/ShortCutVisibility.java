package org.edu_sharing.alfresco.service.config.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ShortCutVisibility {
    VISIBLE("visible"),
    HIDDEN("hidden");


    private final String name;

    ShortCutVisibility(String name){
        this.name = name;
    }

    @JsonCreator
    public static ShortCutVisibility forValue(String value) {
        for (ShortCutVisibility v : values()) {
            if (v.name.equalsIgnoreCase(value)) {
                return v;
            }
        }
        throw new IllegalArgumentException("Unknown value: " + value);
    }

    @JsonValue
    @Override
    public String toString() {
        return name;
    }
}
