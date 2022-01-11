/**
 *
 *  
 * 
 * 
 *	
 *
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 *
 */

package de.metaventis.webservices.translate;

public interface TranslateService extends javax.xml.rpc.Service {
    public java.lang.String getTranslateAddress();

    public de.metaventis.webservices.translate.Translate getTranslate() throws javax.xml.rpc.ServiceException;

    public de.metaventis.webservices.translate.Translate getTranslate(java.net.URL portAddress) throws javax.xml.rpc.ServiceException;
}
