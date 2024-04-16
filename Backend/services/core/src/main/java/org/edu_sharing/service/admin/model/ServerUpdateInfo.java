package org.edu_sharing.service.admin.model;

import lombok.Data;

@Data
public class ServerUpdateInfo {

    String id;
    String description;
    int order;
    boolean auto;
    boolean testable;
    long executedAt;


    public ServerUpdateInfo(String id, String description, int order, boolean auto, boolean testable) {
        this.id = id;
        this.description = description;
        this.order = order;
        this.auto = auto;
        this.testable = testable;
    }
}
