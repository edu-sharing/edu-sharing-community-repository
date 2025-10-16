package org.edu_sharing.repository.server.jobs;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Duration;
import java.util.Date;

@Data
@AllArgsConstructor
public class JobQueueEntry {
    Long id;
    boolean unique;
    String group;
    Date requested;
    Date lastUpdated;
    JobStatus status;
    Duration ttl;


    int jobHash;
    Class<?> bean;
    String method;
    Class<?>[] paramTypes;
    String[] params;
    String user;
}

