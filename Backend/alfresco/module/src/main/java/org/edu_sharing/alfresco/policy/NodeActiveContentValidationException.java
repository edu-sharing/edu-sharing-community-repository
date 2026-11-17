package org.edu_sharing.alfresco.policy;

import lombok.Getter;

import java.util.Set;

/**
 * Thrown when an uploaded file contains active content (e.g. JavaScript inside a PDF) that is blocked via
 * security.fileManagement.contentVerification
 */
@Getter
public class NodeActiveContentValidationException extends RuntimeException {
    private final Set<PdfActiveContentScanner.Finding> findings;

    public NodeActiveContentValidationException(Set<PdfActiveContentScanner.Finding> findings) {
        super("File contains blocked active content: " + findings);
        this.findings = findings;
    }
}
