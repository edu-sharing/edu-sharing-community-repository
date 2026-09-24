package org.edu_sharing.service.upload;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.Config;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.repo.content.ContentStore;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.apache.commons.io.FileUtils;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.alfresco.policy.NodeFileSizeExceededException;
import org.edu_sharing.restservices.DAOException;
import org.edu_sharing.service.InsufficientPermissionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

@Slf4j
@Service("chunkedUploadService")
public class ChunkedUploadServiceImpl implements ChunkedUploadService {

    private static final long DEFAULT_CHUNK_SIZE = 16L * 1024 * 1024;
    private static final long DEFAULT_MAX_CHUNK_SIZE = 64L * 1024 * 1024;
    private static final int DEFAULT_PROCESSING_THREADS = 2;

    private static final Pattern ID_PATTERN = Pattern.compile("[a-fA-F0-9-]{10,64}");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private ContentStore fileContentStore;

    private File baseDir;
    private boolean enabled;
    private long chunkSize;
    private long maxChunkSize;
    private ExecutorService executor;

    @PostConstruct
    public void init() {
        baseDir = new File(new File(fileContentStore.getRootLocation()).getParentFile(), "edu_upload_sessions");
        if (!baseDir.exists() && !baseDir.mkdirs() && !baseDir.exists()) {
            throw new IllegalStateException("unable to create upload session directory " + baseDir.getAbsolutePath());
        }

        Config cfg = LightbendConfigLoader.get();
        enabled = !cfg.hasPath("security.fileManagement.chunked.enabled") || cfg.getBoolean("security.fileManagement.chunked.enabled");
        chunkSize = cfg.hasPath("security.fileManagement.chunked.chunkSize") ? cfg.getLong("security.fileManagement.chunked.chunkSize") : DEFAULT_CHUNK_SIZE;
        maxChunkSize = cfg.hasPath("security.fileManagement.chunked.maxChunkSize") ? cfg.getLong("security.fileManagement.chunked.maxChunkSize") : DEFAULT_MAX_CHUNK_SIZE;
        int processingThreads = cfg.hasPath("security.fileManagement.chunked.processingThreads") ? cfg.getInt("security.fileManagement.chunked.processingThreads") : DEFAULT_PROCESSING_THREADS;

        executor = Executors.newFixedThreadPool(Math.max(1, processingThreads));
    }

    @PreDestroy
    public void destroy() {
        if (executor != null) {
            executor.shutdown();
        }
    }

    @Override
    public UploadSession initUpload(String nodeId, long size, String versionComment) {
        if (!enabled) {
            throw new UnsupportedOperationException("chunked upload is disabled (repository.upload.chunked.enabled=false)");
        }
        if (size < 0) {
            throw new IllegalArgumentException("size must not be negative");
        }
        checkMaxFileSize(size);

        String uploadId = UUID.randomUUID().toString();
        UploadSessionMeta meta = new UploadSessionMeta();
        meta.setId(uploadId);
        meta.setOwner(AuthenticationUtil.getFullyAuthenticatedUser());
        meta.setNodeId(nodeId);
        meta.setVersionComment(versionComment);
        meta.setSize(size);
        meta.setChunkSize(chunkSize);
        meta.setState(UploadSessionState.UPLOADING);
        long now = System.currentTimeMillis();
        meta.setCreated(now);
        meta.setUpdated(now);

        File dir = sessionDir(uploadId);
        if (!dir.mkdirs()) {
            throw new IllegalStateException("unable to create upload session directory " + dir.getAbsolutePath());
        }
        try {
            if (!dataFile(uploadId).createNewFile()) {
                throw new IllegalStateException("upload session data file already exists: " + uploadId);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        saveMeta(meta);
        log.info("initialized chunked upload {} for node {}, size {} bytes", uploadId, nodeId, size);
        return toSession(meta);
    }

    @Override
    public UploadSession appendChunk(String uploadId, String nodeId, long offset, InputStream data) {
        UploadSessionMeta meta = loadMeta(uploadId);
        checkNode(meta, nodeId);
        checkOwner(meta);
        if (meta.getState() != UploadSessionState.UPLOADING) {
            throw new IllegalStateException("upload session " + uploadId + " is not accepting chunks (state=" + meta.getState() + ")");
        }

        File dataFile = dataFile(uploadId);
        try (RandomAccessFile raf = new RandomAccessFile(dataFile, "rw");
             FileChannel channel = raf.getChannel();
             FileLock lock = channel.lock()) {

            long currentOffset = channel.size();
            if (currentOffset != offset) {
                throw new UploadChunkOffsetMismatchException(uploadId, currentOffset);
            }
            channel.position(currentOffset);

            byte[] buffer = new byte[64 * 1024];
            long written = 0;
            int read;
            try {
                while ((read = data.read(buffer)) != -1) {
                    written += read;
                    if (currentOffset + written > meta.getSize() || written > maxChunkSize) {
                        throw new IllegalArgumentException(
                                "chunk exceeds the declared upload size or the configured max chunk size of " + maxChunkSize + " bytes");
                    }
                    channel.write(ByteBuffer.wrap(buffer, 0, read));
                }
            } catch (IOException | RuntimeException e) {
                channel.truncate(currentOffset);
                throw e;
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        meta.setUpdated(System.currentTimeMillis());
        saveMeta(meta);
        return toSession(meta);
    }

    @Override
    public UploadSession getSession(String uploadId, String nodeId) {
        UploadSessionMeta meta = loadMeta(uploadId);
        checkNode(meta, nodeId);
        checkOwner(meta);
        return toSession(meta);
    }

    @Override
    public UploadSession completeUpload(String uploadId, String nodeId, UploadContentWriter writer) {
        UploadSessionMeta meta = loadMeta(uploadId);
        checkNode(meta, nodeId);
        checkOwner(meta);

        if (meta.getState() == UploadSessionState.PROCESSING || meta.getState() == UploadSessionState.DONE) {
            return toSession(meta);
        }
        if (meta.getState() != UploadSessionState.UPLOADING) {
            throw new IllegalStateException("upload session " + uploadId + " can not be completed (state=" + meta.getState() + ")");
        }

        long actualSize = dataFile(uploadId).length();
        if (actualSize != meta.getSize()) {
            throw new IllegalStateException("upload session " + uploadId + " is incomplete: expected "
                    + meta.getSize() + " bytes, got " + actualSize);
        }

        meta.setState(UploadSessionState.PROCESSING);
        meta.setUpdated(System.currentTimeMillis());
        meta.setProcessingHeartbeat(System.currentTimeMillis());
        saveMeta(meta);

        executor.submit(() -> process(uploadId, writer));

        return toSession(meta);
    }

    private void process(String uploadId, UploadContentWriter writer) {
        UploadSessionMeta meta;
        try {
            meta = loadMeta(uploadId);
        } catch (UploadSessionNotFoundException e) {
            log.warn("upload session {} disappeared before processing could start", uploadId);
            return;
        }
        try {
            AuthenticationUtil.runAs((AuthenticationUtil.RunAsWork<Void>) () -> {
                writer.write(dataFile(uploadId), meta.getVersionComment());
                return null;
            }, meta.getOwner());
            meta.setState(UploadSessionState.DONE);
            meta.setErrorCode(null);
            meta.setErrorMessage(null);
            meta.setErrorDetails(null);
            log.info("chunked upload {} for node {} completed", uploadId, meta.getNodeId());
        } catch (Throwable t) {
            log.error("processing chunked upload {} for node {} failed", uploadId, meta.getNodeId(), t);
            meta.setState(UploadSessionState.FAILED);
            meta.setErrorCode(t.getClass().getName());
            meta.setErrorMessage(t.getMessage());
            meta.setErrorDetails(t instanceof DAOException ? ((DAOException) t).getDetails() : null);
        }
        meta.setUpdated(System.currentTimeMillis());
        saveMeta(meta);
        FileUtils.deleteQuietly(dataFile(uploadId));
    }

    @Override
    public void abortUpload(String uploadId, String nodeId) {
        UploadSessionMeta meta = loadMeta(uploadId);
        checkNode(meta, nodeId);
        checkOwner(meta);
        if (meta.getState() == UploadSessionState.PROCESSING || meta.getState() == UploadSessionState.DONE) {
            // completion has already started (all bytes were received) or finished - let it run its
            // course instead of racing a delete against the background thread reading dataFile(uploadId),
            // and instead of undoing a content update that already committed. It cleans up after itself.
            log.info("ignoring abort for chunked upload {} for node {}, already {}", uploadId, nodeId, meta.getState());
            return;
        }
        FileUtils.deleteQuietly(sessionDir(uploadId));
        log.info("aborted chunked upload {} for node {}", uploadId, nodeId);
    }

    @Override
    public void cleanup(long ttlMillis) {
        File[] dirs = baseDir.listFiles(File::isDirectory);
        if (dirs == null) {
            return;
        }
        long now = System.currentTimeMillis();
        for (File dir : dirs) {
            String uploadId = dir.getName();
            UploadSessionMeta meta;
            try {
                meta = loadMeta(uploadId);
            } catch (Exception e) {
                log.warn("removing orphaned/corrupt upload session directory {}", dir.getAbsolutePath());
                FileUtils.deleteQuietly(dir);
                continue;
            }
            boolean stuckProcessing = meta.getState() == UploadSessionState.PROCESSING
                    && (now - meta.getProcessingHeartbeat()) > ttlMillis;
            if (stuckProcessing) {
                log.warn("upload session {} stuck in PROCESSING, marking as FAILED", uploadId);
                meta.setState(UploadSessionState.FAILED);
                meta.setErrorCode("processing_timeout");
                meta.setErrorMessage("processing timed out");
                meta.setUpdated(now);
                saveMeta(meta);
            } else if ((now - meta.getUpdated()) > ttlMillis) {
                log.info("removing stale upload session {} (inactive since {})", uploadId, meta.getUpdated());
                FileUtils.deleteQuietly(dir);
            }
        }
    }

    private void checkMaxFileSize(long size) {
        Config cfg = LightbendConfigLoader.get();
        if (cfg.hasPath("security.fileManagement.limits.fileSize") && !cfg.getIsNull("security.fileManagement.limits.fileSize")) {
            long maxSize = cfg.getLong("security.fileManagement.limits.fileSize");
            if (size > maxSize) {
                throw new NodeFileSizeExceededException(maxSize, size);
            }
        }
    }

    private void checkNode(UploadSessionMeta meta, String nodeId) {
        if (!meta.getNodeId().equals(nodeId)) {
            // do not leak that the session exists for a different node
            throw new UploadSessionNotFoundException(meta.getId());
        }
    }

    private void checkOwner(UploadSessionMeta meta) {
        String user = AuthenticationUtil.getFullyAuthenticatedUser();
        if (!meta.getOwner().equals(user)) {
            throw new InsufficientPermissionException("upload session " + meta.getId() + " does not belong to user " + user);
        }
    }

    private File sessionDir(String uploadId) {
        if (!ID_PATTERN.matcher(uploadId).matches()) {
            throw new UploadSessionNotFoundException(uploadId);
        }
        return new File(baseDir, uploadId);
    }

    private File dataFile(String uploadId) {
        return new File(sessionDir(uploadId), "data.bin");
    }

    private File metaFile(String uploadId) {
        return new File(sessionDir(uploadId), "meta.json");
    }

    private UploadSessionMeta loadMeta(String uploadId) {
        File file = metaFile(uploadId);
        if (!file.exists()) {
            throw new UploadSessionNotFoundException(uploadId);
        }
        try {
            return MAPPER.readValue(file, UploadSessionMeta.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void saveMeta(UploadSessionMeta meta) {
        try {
            MAPPER.writeValue(metaFile(meta.getId()), meta);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private UploadSession toSession(UploadSessionMeta meta) {
        UploadSession session = new UploadSession();
        session.setId(meta.getId());
        session.setNodeId(meta.getNodeId());
        session.setSize(meta.getSize());
        session.setChunkSize(meta.getChunkSize());
        session.setState(meta.getState());
        session.setErrorCode(meta.getErrorCode());
        session.setErrorMessage(meta.getErrorMessage());
        session.setErrorDetails(meta.getErrorDetails());
        session.setOffset(meta.getState() == UploadSessionState.UPLOADING || meta.getState() == UploadSessionState.PROCESSING
                ? dataFile(meta.getId()).length()
                : meta.getSize());
        return session;
    }
}
