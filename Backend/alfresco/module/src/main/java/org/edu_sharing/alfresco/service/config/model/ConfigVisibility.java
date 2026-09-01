package org.edu_sharing.alfresco.service.config.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ConfigVisibility {
    VISIBLE("visible"),
    HIDDEN("hidden");


    private final String name;

    ConfigVisibility(String name){
        this.name = name;
    }

    @JsonCreator
    public static ConfigVisibility forValue(String value) {
        for (ConfigVisibility v : values()) {
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
