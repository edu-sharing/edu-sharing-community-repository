package org.edu_sharing.restservices.iam.v1.model;

import lombok.Data;
import org.edu_sharing.restservices.NodeDao;
import org.edu_sharing.restservices.node.v1.model.NodeEntry;
import org.edu_sharing.service.dataprotection.queue.DataProtectionQueueEntry;

@Data
public class DataProtectionExport {
    DataProtectionQueueEntry statusObject;
    NodeEntry nodeEntry;
}
