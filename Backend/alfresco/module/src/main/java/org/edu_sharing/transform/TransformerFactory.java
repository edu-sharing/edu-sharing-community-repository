package org.edu_sharing.transform;

import javax.xml.XMLConstants;
import javax.xml.transform.TransformerConfigurationException;

public class TransformerFactory extends org.apache.xalan.processor.TransformerFactoryImpl{

    public TransformerFactory(){
        super();
        try {
            this.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING,true);
        } catch (TransformerConfigurationException e) {
            throw new RuntimeException(e);
        }
    }
}
