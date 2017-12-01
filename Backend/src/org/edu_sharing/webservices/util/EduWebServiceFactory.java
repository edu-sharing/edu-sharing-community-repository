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
package org.edu_sharing.webservices.util;

import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.rpc.ServiceException;

import org.apache.log4j.Logger;
import org.edu_sharing.webservices.alfresco.extension.NativeAlfrescoWrapper;
import org.edu_sharing.webservices.alfresco.extension.NativeAlfrescoWrapperServiceLocator;
import org.edu_sharing.webservices.authentication.Authentication;
import org.edu_sharing.webservices.authentication.AuthenticationServiceLocator;
import org.edu_sharing.webservices.crud.Crud;
import org.edu_sharing.webservices.crud.CrudServiceLocator;


public class EduWebServiceFactory {
	static Logger logger = Logger.getLogger(EduWebServiceFactory.class);
	
	
	public static NativeAlfrescoWrapper getNativeAlfrescoWrapper(String wsurl){
		try{
			return new NativeAlfrescoWrapperServiceLocator(AuthenticationUtils.getEngineConfiguration()).getNativeAlfrescoWrapper(new URL(wsurl+"NativeAlfrescoWrapper"));
		}catch(ServiceException e){
			//e.printStackTrace();
			logger.error(e.getMessage(), e);
			
		}catch(MalformedURLException e){
			logger.error(e.getMessage(), e);
			
		}
		
		return null;
	}
	
	/**
	 * 
	 * @param endpointAddress (the full url to the service)
	 * @return
	 */
	public static Authentication getAuthenticationServiceByEndpointAddress(String endpointAddress){
		try{
			AuthenticationServiceLocator locator = new AuthenticationServiceLocator();
			locator.setauthenticationEndpointAddress(endpointAddress);
			return locator.getauthentication();
		}catch(ServiceException e){
			//e.printStackTrace();
			logger.error(e.getMessage(), e);
			
		}
		return null;
	}
	
	public static Crud getCrud(String wsurl){
		try{
			CrudServiceLocator locator = new CrudServiceLocator();
			locator.setcrudEndpointAddress(wsurl+"crud");
			return locator.getcrud();
		}catch(ServiceException e){
			//e.printStackTrace();
			logger.error(e.getMessage(), e);
			return null;
		}
	}
}
