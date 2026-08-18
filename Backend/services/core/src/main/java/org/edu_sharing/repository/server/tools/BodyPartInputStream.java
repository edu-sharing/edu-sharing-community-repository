package org.edu_sharing.repository.server.tools;

import org.glassfish.jersey.media.multipart.BodyPart;
import org.glassfish.jersey.media.multipart.BodyPartEntity;

import java.io.IOException;
import java.io.InputStream;

/**
 * An {@link InputStream} on a multipart body part that can be read more than once.
 * <p>
 * A plain {@code @FormDataParam InputStream} can only be consumed once, which makes any kind of retry
 * impossible: the second attempt silently reads zero bytes. Jersey however already buffers each body part
 * (in memory up to 1 MB, larger parts in a temp file of its own) and hands out a new stream on that buffer for
 * every {@link BodyPartEntity#getInputStream()} call, so {@link #reset()} simply asks for a fresh one instead of
 * buffering the data a second time. The buffer is released by Jersey when the request ends.
 * <p>
 * The stream is not thread safe.
 */
public class BodyPartInputStream extends InputStream {

    private final BodyPartEntity entity;
    private InputStream delegate;

    public BodyPartInputStream(BodyPart bodyPart) {
        this(bodyPart.getEntityAs(BodyPartEntity.class));
    }

    public BodyPartInputStream(BodyPartEntity entity) {
        this.entity = entity;
        delegate = entity.getInputStream();
    }

    @Override
    public int read() throws IOException {
        return delegate().read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return delegate().read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        return delegate().skip(n);
    }

    @Override
    public int available() throws IOException {
        return delegate().available();
    }

    @Override
    public boolean markSupported() {
        return true;
    }

    /**
     * no-op, {@link #reset()} always returns to the beginning of the body part
     */
    @Override
    public void mark(int readLimit) {
        // nothing to remember, jersey keeps the whole part
    }

    /**
     * Starts over at the first byte, also after this stream has been closed by a consumer
     */
    @Override
    public void reset() {
        closeQuietly();
        delegate = entity.getInputStream();
    }

    /**
     * Only closes the current read position, the body part stays readable via {@link #reset()}
     */
    @Override
    public void close() throws IOException {
        if (delegate != null) {
            InputStream toClose = delegate;
            delegate = null;
            toClose.close();
        }
    }

    private InputStream delegate() throws IOException {
        if (delegate == null) {
            throw new IOException("Stream is closed, call reset() to read the body part again");
        }
        return delegate;
    }

    private void closeQuietly() {
        try {
            close();
        } catch (IOException ignored) {
            // the position is replaced anyway
        }
    }
}
