package org.edu_sharing.xml.security.validation;

import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.validation.Schema;

public class SchemaFactory extends javax.xml.validation.SchemaFactory{


    java.util.logging.Logger logger =  java.util.logging.Logger.getLogger(this.getClass().getName());

    com.sun.org.apache.xerces.internal.jaxp.validation.XMLSchemaFactory wrapped = new com.sun.org.apache.xerces.internal.jaxp.validation.XMLSchemaFactory();


    public SchemaFactory(){
        logger.fine("using SchemaFactory "+SchemaFactory.class.getName());
        try {
            wrapped.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA,"jar:file,file");
            wrapped.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD,"");

        } catch (SAXNotRecognizedException e) {
            throw new RuntimeException(e);
        } catch (SAXNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isSchemaLanguageSupported(String schemaLanguage) {
        return wrapped.isSchemaLanguageSupported(schemaLanguage);
    }

    @Override
    public void setErrorHandler(ErrorHandler errorHandler) {
        wrapped.setErrorHandler(errorHandler);
    }

    @Override
    public ErrorHandler getErrorHandler() {
        return wrapped.getErrorHandler();
    }

    @Override
    public void setResourceResolver(LSResourceResolver resourceResolver) {
        wrapped.setResourceResolver(resourceResolver);
    }

    @Override
    public LSResourceResolver getResourceResolver() {
        return wrapped.getResourceResolver();
    }

    @Override
    public Schema newSchema(Source[] schemas) throws SAXException {
        return wrapped.newSchema(schemas);
    }

    @Override
    public Schema newSchema() throws SAXException {
        return wrapped.newSchema();
    }

    @Override
    public void setFeature(String name, boolean value) throws SAXNotRecognizedException, SAXNotSupportedException {
        wrapped.setFeature(name, value);
    }

    @Override
    public void setProperty(String name, Object object) throws SAXNotRecognizedException, SAXNotSupportedException {
        wrapped.setProperty(name, object);
    }
}
