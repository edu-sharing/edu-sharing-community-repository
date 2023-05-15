package org.edu_sharing.xml.security.sax;

import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

/**
 * use with system property: -Dorg.xml.sax.driver=org.edu_sharing.xml.security.sax.XMLReader
 * or serviceloader file: org.xml.sax.XMLReader
 *
 * when using edu-sharing cluster plugin hazelcast tries to set
 * Feature 'http://javax.xml.XMLConstants/feature/secure-processing' and a warning 'is not recognized' appears in log.
 *
 * we use the owasp recommandations here. FEATURE_SECURE_PROCESSING goes not far enough:
 * https://cheatsheetseries.owasp.org/cheatsheets/XML_External_Entity_Prevention_Cheat_Sheet.html
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
