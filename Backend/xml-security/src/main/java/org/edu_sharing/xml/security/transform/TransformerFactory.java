package org.edu_sharing.xml.security.transform;

import javax.xml.XMLConstants;
import javax.xml.transform.TransformerConfigurationException;

public class TransformerFactory extends com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl{

    java.util.logging.Logger logger =  java.util.logging.Logger.getLogger(this.getClass().getName());
    public TransformerFactory(){
        super();
        logger.fine("using TransformerFactory:" +TransformerFactory.class.getName());
        try {
            this.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING,true);
            this.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            this.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET,"");
        } catch (TransformerConfigurationException e) {
            throw new RuntimeException(e);
        }
    }
}
