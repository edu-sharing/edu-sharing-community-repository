package org.edu_sharing.service.upload;

public class UploadSessionNotFoundException extends RuntimeException {
    public UploadSessionNotFoundException(String uploadId) {
        super("upload session not found: " + uploadId);
    }
}
