package org.edu_sharing.xml.security.xpath;


import javax.xml.XMLConstants;
import javax.xml.xpath.XPathFactoryConfigurationException;

public class XPathFactory extends org.apache.xpath.jaxp.XPathFactoryImpl {

    public XPathFactory(){

        try {
            setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        } catch (XPathFactoryConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

}
