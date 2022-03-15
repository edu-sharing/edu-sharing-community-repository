package org.edu_sharing.service.relations;

import lombok.Data;

import java.util.Date;

@Data
public class RelationData {
    private String node;
    private String creator;
    private Date timestamp;
    private OutputRelationType type;
}
