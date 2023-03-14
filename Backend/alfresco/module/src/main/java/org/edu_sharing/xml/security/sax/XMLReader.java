package org.edu_sharing.xml.security.sax;

import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

public class XMLReader extends org.apache.xerces.parsers.SAXParser{
    public XMLReader(){
        super();
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
