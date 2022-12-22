package org.edu_sharing.alfresco.policy;

import java.util.List;

public class NodeFileExtensionValidationException extends RuntimeException {
    public NodeFileExtensionValidationException(List<String> allowedExtensions, String detectedExtension) {
        super();
    }
}
