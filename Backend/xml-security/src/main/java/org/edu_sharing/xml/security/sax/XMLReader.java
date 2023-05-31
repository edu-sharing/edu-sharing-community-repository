package org.edu_sharing.xml.security.sax;

import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

/**
 * use with system property: -Dorg.xml.sax.driver=org.edu_sharing.xml.security.sax.XMLReader
 * or serviceloader file: org.xml.sax.driver
 *
 * when using edu-sharing cluster plugin and System Property org.xml.sax.driver (not service loader +) hazelcast tries to set
 * Feature 'http://javax.xml.XMLConstants/feature/secure-processing' and a warning 'is not recognized' appears in log.
 *
 * this class uses the owasp recommandations here. FEATURE_SECURE_PROCESSING goes not far enough:
 * https://cheatsheetseries.owasp.org/cheatsheets/XML_External_Entity_Prevention_Cheat_Sheet.html
 *
 * + hazelcast (current 3.12.13) uses jdk.xml.internal.JdkXmlUtils#getXMLReader to get XMLReader this only looks on System Property
 *   not serviceloader file for org.xml.sax.driver. when systemproperty is not set the following factory class
 *   com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl with the XMLReader:
 *   com.sun.org.apache.xerces.internal.jaxp.SAXParserImpl$JAXPSAXParser is used.
 *   this XMLReader Implementation supports  FEATURE_SECURE_PROCESSING
 *
 * XMLReader xmlReader = XMLReaderFactory.createXMLReader(); (org.xml.sax.helpers.XMLReaderFactory)
 * uses the serviceloader
 */
public class XMLReader extends org.apache.xerces.parsers.SAXParser{
    java.util.logging.Logger logger =  java.util.logging.Logger.getLogger(this.getClass().getName());
    public XMLReader(){
        super();

        logger.fine("using XMLReader:" + XMLReader.class.getName());

        try {
            super.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            // This may not be strictly required as DTDs shouldn't be allowed at all, per previous line.
            super.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            super.setFeature("http://xml.org/sax/features/external-general-entities", false);
            super.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        } catch (SAXNotRecognizedException e) {
            throw new RuntimeException(e);
        } catch (SAXNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
