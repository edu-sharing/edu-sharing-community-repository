package org.edu_sharing.alfresco.service.search.cmis;

import lombok.Getter;

@Getter
public class Selection {

    private final Property[] properties;

    public Selection(Property... properties) {
        this.properties = properties;
    }

    public Table from(String name) {
        return new Table(this, name);
    }

}
