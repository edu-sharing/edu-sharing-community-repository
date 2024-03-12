package org.edu_sharing.xml.security.jaxp;

import javax.xml.XMLConstants;

public class DocumentBuilderFactory extends com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl{

    java.util.logging.Logger logger =  java.util.logging.Logger.getLogger(this.getClass().getName());

    public DocumentBuilderFactory(){
        super();
        logger.fine("using DocumentBuilderFactory:" + DocumentBuilderFactory.class.getName());
        this.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");

        //don't set this cause alfresco spring beans will not be loaded
        //this.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    }
}
