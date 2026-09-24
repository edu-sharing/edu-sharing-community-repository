package org.edu_sharing.service.upload;

import java.io.File;

/**
 * Supplied by the caller of {@link ChunkedUploadService#completeUpload}: writes the assembled
 * upload file to its final destination (e.g. node content). Runs on a background thread once all
 * chunks have been received, so this service stays agnostic of how/where the file is stored.
 */
@FunctionalInterface
public interface UploadContentWriter {
    void write(File file, String versionComment) throws Exception;
}
