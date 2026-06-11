package org.edu_sharing.repository.server.jobs.quartz;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.server.importer.PersistenHandlerKeywordsDNBMarc;
import org.edu_sharing.repository.server.importer.RecordHandlerKeywordsDNBMarc;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.*;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Streams a DNB MARC xml file record by record using StAX and delegates the actual
 * parsing to {@link RecordHandlerKeywordsDNBMarc}. Only a single &lt;record&gt; block is
 * buffered/parsed into a DOM at a time, so the full document tree is never loaded into
 * memory (same reason the previous SAX implementation existed), while the field/property
 * logic lives in exactly one place.
 */
public class ImportFactualTermsFromFileStax {

    Logger logger = Logger.getLogger(ImportFactualTermsFromFileStax.class);

    PersistenHandlerKeywordsDNBMarc persistentHandler;
    AbstractJob job;

    RecordHandlerKeywordsDNBMarc recordHandler = new RecordHandlerKeywordsDNBMarc(null);

    XMLOutputFactory outFactory = XMLOutputFactory.newInstance();
    DocumentBuilder documentBuilder;

    int processedKeywords = 0;
    List<String> errorKeywords = new ArrayList<>();

    public ImportFactualTermsFromFileStax(URI uri, PersistenHandlerKeywordsDNBMarc persistentHandler, AbstractJob job) throws Throwable {
        this.persistentHandler = persistentHandler;
        this.job = job;

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        // keep namespace awareness off: the XPaths in RecordHandlerKeywordsDNBMarc are not
        // namespace aware and match the (unprefixed) MARC slim elements by qualified name.
        dbFactory.setNamespaceAware(false);
        this.documentBuilder = dbFactory.newDocumentBuilder();

        XMLInputFactory factory = XMLInputFactory.newInstance();

        InputStream is = uri.toURL().openStream();
        XMLStreamReader reader = factory.createXMLStreamReader(is);

        StringWriter buffer = null;
        XMLStreamWriter writer = null;

        try {
            while (reader.hasNext()) {

                if (this.job.isInterrupted()) {
                    logger.info("job was interrupted");
                    break;
                }

                int event = reader.next();

                if (event == XMLStreamConstants.START_ELEMENT
                        && "record".equals(reader.getLocalName())) {

                    buffer = new StringWriter();
                    writer = outFactory.createXMLStreamWriter(buffer);
                    writer.writeStartDocument();
                }

                if (writer != null) {
                    writeEvent(reader, writer);
                }

                if (event == XMLStreamConstants.END_ELEMENT
                        && "record".equals(reader.getLocalName())) {

                    writer.writeEndDocument();
                    writer.close();

                    String xml = buffer.toString();

                    handleRecord(toDom(xml));

                    // reset so events between records are not written
                    writer = null;
                    buffer = null;
                }
            }
        } finally {
            reader.close();
            is.close();
            logger.info("finished:" + processedKeywords + " processed." + errorKeywords.size() + " errors");
            if (errorKeywords.size() > 0) {
                logger.error(StringUtils.join(errorKeywords, ", "));
            }
        }
    }

    /**
     * Copies the reader's current event 1:1 into the writer so that each buffered
     * &lt;record&gt; block is a self contained, well formed xml string.
     */
    private void writeEvent(XMLStreamReader reader, XMLStreamWriter writer) throws XMLStreamException {
        switch (reader.getEventType()) {
            case XMLStreamConstants.START_ELEMENT:
                writer.writeStartElement(reader.getLocalName());
                for (int i = 0; i < reader.getNamespaceCount(); i++) {
                    writer.writeNamespace(reader.getNamespacePrefix(i), reader.getNamespaceURI(i));
                }
                for (int i = 0; i < reader.getAttributeCount(); i++) {
                    writer.writeAttribute(reader.getAttributeLocalName(i), reader.getAttributeValue(i));
                }
                break;
            case XMLStreamConstants.CHARACTERS:
            case XMLStreamConstants.CDATA:
            case XMLStreamConstants.SPACE:
                writer.writeCharacters(reader.getText());
                break;
            case XMLStreamConstants.END_ELEMENT:
                writer.writeEndElement();
                break;
            default:
                break;
        }
    }

    private Document toDom(String xml) throws Exception {
        documentBuilder.reset();
        return documentBuilder.parse(new InputSource(new StringReader(xml)));
    }

    private void handleRecord(Document doc) {
        try {
            // pass the <record> element: the handler's XPaths are relative to the record node
            recordHandler.handleRecord(doc.getDocumentElement(), "", "");
            persistentHandler.safe(recordHandler, "", "");
            processedKeywords++;
        } catch (Throwable throwable) {
            Object id = recordHandler.getProperties().get(RecordHandlerKeywordsDNBMarc.ID);
            errorKeywords.add(id == null ? "unknown" : id.toString());
            logger.error(throwable.getMessage(), throwable);
        }
    }
}
