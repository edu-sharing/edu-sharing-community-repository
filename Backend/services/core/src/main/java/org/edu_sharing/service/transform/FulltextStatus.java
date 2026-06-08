package org.edu_sharing.service.transform;

public enum FulltextStatus {
    CONTENT_AVAILABLE,
    NO_CONTENT,
    // local alfresco transformer failed with a generic error
    TRANSFORM_ERROR_INTERNAL,
    // local alfresco transformer has no handler for this mimetype
    TRANSFORM_ERROR_UNSUPPORTED,
    // i.e. external service like b-api
    TRANSFORM_ERROR_EXTERNAL,
}
