package org.edu_sharing.service.oai.formats;

import io.gdcc.xoai.dataprovider.model.MetadataFormat;
import org.alfresco.service.cmr.repository.NodeRef;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.junit.jupiter.api.Test;

import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Regression test for the empty-metadata bug that produced a cryptic
 * {@code io.gdcc.xoai.xmlio.exceptions.XmlWriteException: Unexpected EOF in prolog}
 * when an OAI record's metadata string was empty.
 *
 * <p>Previously {@link AbstractMetadataFormatWriter#write} silently returned without
 * writing anything when the node type was missing or not {@code ccm:io}, leaving an
 * empty metadata string that XOAI later failed to parse. It must now fail loudly.</p>
 */
class AbstractMetadataFormatWriterTest {

    private static final NodeRef NODE_REF = new NodeRef("workspace://SpacesStore/test-node");

    /** Minimal concrete writer; {@link #build} is never reached for an invalid type. */
    private static AbstractMetadataFormatWriter writer() {
        return new AbstractMetadataFormatWriter() {
            @Override
            public MetadataFormat getFormat() {
                return MetadataFormat.metadataFormat("dc").withTransformer(MetadataFormat.identity());
            }

            @Override
            public void build(Context context) throws ParserConfigurationException {
                // not exercised by the invalid-type cases below
            }
        };
    }

    @Test
    void throwsWhenNodeTypeMissing() {
        assertThrows(IllegalStateException.class,
                () -> writer().write(new ByteArrayOutputStream(), NODE_REF, new HashMap<>()));
    }

    @Test
    void throwsWhenNodeTypeNotIo() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(CCConstants.NODETYPE, "ccm:map");
        assertThrows(IllegalStateException.class,
                () -> writer().write(new ByteArrayOutputStream(), NODE_REF, properties));
    }
}
