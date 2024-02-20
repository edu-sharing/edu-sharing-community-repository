package org.edu_sharing.alfresco.policy;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NodeFileSizeExceededException extends RuntimeException {
    private final long maxSize;
    private final long actualSize;

}
