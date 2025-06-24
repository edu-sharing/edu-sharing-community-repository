package org.edu_sharing.alfresco.service.config.model;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

public class ShortCutVisibilityAdapter extends XmlAdapter<String, ShortCutVisibility> {
    @Override
    public ShortCutVisibility unmarshal(String v) {
        for (ShortCutVisibility visibility : ShortCutVisibility.values()) {
            if (visibility.toString().equalsIgnoreCase(v)) {
                return visibility;
            }
        }
        throw new IllegalArgumentException("Unknown visibility: " + v);
    }

    @Override
    public String marshal(ShortCutVisibility v) {
        return v.toString();
    }
}
