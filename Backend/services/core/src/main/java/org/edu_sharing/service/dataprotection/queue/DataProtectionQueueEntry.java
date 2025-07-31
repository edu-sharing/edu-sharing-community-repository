package org.edu_sharing.service.dataprotection.queue;

import lombok.Data;

import java.util.Date;

@Data
public class DataProtectionQueueEntry {
    String user;
    String status;
    Date requested;
    String node_id;
    Date finished;
}
