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

public interface Translate extends java.rmi.Remote {
    public java.lang.String version(java.lang.String type) throws java.rmi.RemoteException;
    public java.lang.String[] translate(java.lang.String tokenKind, java.lang.String[] tokenText, java.lang.String tokenLevel, java.lang.String tokenLang, java.lang.String tokenCleanup, java.lang.String classKind, java.lang.String classLevel, java.lang.String classLang, java.lang.String returnLength) throws java.rmi.RemoteException;
}
