package org.edu_sharing.xml.security.xpath;


import javax.xml.XMLConstants;
import javax.xml.xpath.XPathFactoryConfigurationException;

public class XPathFactory extends org.apache.xpath.jaxp.XPathFactoryImpl {

    java.util.logging.Logger logger =  java.util.logging.Logger.getLogger(this.getClass().getName());

    public XPathFactory(){
        logger.fine("using XPathFactory:" +XPathFactory.class.getName());

        try {
            setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        } catch (XPathFactoryConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

}
