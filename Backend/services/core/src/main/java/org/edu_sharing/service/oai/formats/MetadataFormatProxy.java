package org.edu_sharing.service.oai.formats;

import io.gdcc.xoai.dataprovider.model.MetadataFormat;

import javax.xml.transform.Transformer;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * A {@link MetadataFormat} that obtains its {@link Transformer} from an injected factory on every
 * {@link #getTransformer()} call instead of handing out a single cached instance.
 *
 * <p>{@code javax.xml.transform.Transformer} is not thread-safe. The default XOAI wiring caches one
 * transformer per format in a Spring singleton; both {@link AbstractMetadataFormatWriter#write} and
 * XOAI's own {@code MetadataHelper.process} then call {@code transform()} on that shared instance
 * concurrently, producing empty/corrupted metadata under load
 * (WstxEOFException: Unexpected EOF in prolog). Delegating {@code getTransformer()} to a factory
 * (e.g. {@code MetadataFormat::identity}) removes the shared mutable state.</p>
 */
public class MetadataFormatProxy extends MetadataFormat {

    private final Supplier<Transformer> transformerFactory;

    public MetadataFormatProxy(Supplier<Transformer> transformerFactory) {
        this.transformerFactory = Objects.requireNonNull(transformerFactory, "transformerFactory");
    }

    @Override
    public Transformer getTransformer() {
        return transformerFactory.get();
    }
}
