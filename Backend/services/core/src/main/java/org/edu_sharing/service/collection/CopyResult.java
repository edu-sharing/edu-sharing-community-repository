package org.edu_sharing.service.collection;

import lombok.Data;
import org.alfresco.service.cmr.repository.NodeRef;

import java.util.ArrayList;

@Data
public class CopyResult {

    NodeRef root;

    ArrayList<Entry> entries = new ArrayList<>();

    public record Entry(String id, ErrorCode error) {}

    public enum ErrorCode {
        NO_PUBLISH_PERMISSION,
        NO_RIGHTS_ON_PERMISSIONS,
        UNKNOWN_ERROR
    }
}
