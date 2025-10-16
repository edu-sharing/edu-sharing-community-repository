package org.edu_sharing.restservices.iam.v1.model;

import lombok.Data;
import org.edu_sharing.restservices.node.v1.model.NodeEntry;

@Data
public class DataProtectionExport {
    NodeEntry nodeEntry;
}
