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

public class TranslateProxy implements de.metaventis.webservices.translate.Translate {
  private String _endpoint = null;
  private de.metaventis.webservices.translate.Translate translate = null;
  
  public TranslateProxy() {
    _initTranslateProxy();
  }
  
  public TranslateProxy(String endpoint) {
    _endpoint = endpoint;
    _initTranslateProxy();
  }
  
  private void _initTranslateProxy() {
    try {
      translate = (new de.metaventis.webservices.translate.TranslateServiceLocator()).getTranslate();
      if (translate != null) {
        if (_endpoint != null)
          ((javax.xml.rpc.Stub)translate)._setProperty("javax.xml.rpc.service.endpoint.address", _endpoint);
        else
          _endpoint = (String)((javax.xml.rpc.Stub)translate)._getProperty("javax.xml.rpc.service.endpoint.address");
      }
      
    }
    catch (javax.xml.rpc.ServiceException serviceException) {}
  }
  
  public String getEndpoint() {
    return _endpoint;
  }
  
  public void setEndpoint(String endpoint) {
    _endpoint = endpoint;
    if (translate != null)
      ((javax.xml.rpc.Stub)translate)._setProperty("javax.xml.rpc.service.endpoint.address", _endpoint);
    
  }
  
  public de.metaventis.webservices.translate.Translate getTranslate() {
    if (translate == null)
      _initTranslateProxy();
    return translate;
  }
  
  public java.lang.String version(java.lang.String type) throws java.rmi.RemoteException{
    if (translate == null)
      _initTranslateProxy();
    return translate.version(type);
  }
  
  public java.lang.String[] translate(java.lang.String tokenKind, java.lang.String[] tokenText, java.lang.String tokenLevel, java.lang.String tokenLang, java.lang.String tokenCleanup, java.lang.String classKind, java.lang.String classLevel, java.lang.String classLang, java.lang.String returnLength) throws java.rmi.RemoteException{
    if (translate == null)
      _initTranslateProxy();
    return translate.translate(tokenKind, tokenText, tokenLevel, tokenLang, tokenCleanup, classKind, classLevel, classLang, returnLength);
  }
  
  
}