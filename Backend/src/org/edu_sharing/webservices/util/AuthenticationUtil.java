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

import java.rmi.RemoteException;

import javax.xml.rpc.ServiceException;

import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.webservices.authentication.Authentication;
import org.edu_sharing.webservices.authentication.AuthenticationException;
import org.edu_sharing.webservices.authentication.AuthenticationResult;
import org.edu_sharing.webservices.authentication.AuthenticationServiceLocator;
import org.edu_sharing.webservices.authentication.AuthenticationSoapBindingStub;


public class AuthenticationUtil {
	
	 /**
     * Start a session with cc authentication webservice AuthMethod TrustedApplication
     * @param remoteapp applicationId
     * @param username
     * @param email
     * @param ticket
     * @param createUser
     * @param appId
     * @throws RemoteException
     */
    public static void startSession(java.lang.String remoteAppId, java.lang.String username, java.lang.String email, java.lang.String ticket, boolean createUser) throws RemoteException{
    	String localAppId = ApplicationInfoList.getHomeRepository().getAppId();
    	System.out.println("startSession remoteApplicationId:"+remoteAppId +" localAppId:"+localAppId);
    	
    	ApplicationInfo appInfoRemoteApp = ApplicationInfoList.getRepositoryInfoById(remoteAppId);
    	String ccAuthWsUrl = appInfoRemoteApp.getAuthenticationwebservice();
    	System.out.println("startSession ccAuthWsUrl:"+ccAuthWsUrl);
    	AuthenticationServiceLocator locator = new AuthenticationServiceLocator();
    	locator.setauthenticationEndpointAddress(ccAuthWsUrl);
    	
		try{
			AuthenticationSoapBindingStub stub = (AuthenticationSoapBindingStub)locator.getPort(Authentication.class);
			AuthenticationResult authResult = stub.authenticateByApp(localAppId, username, email, ticket, createUser);
			AuthenticationUtils.setAuthenticationDetails(new AuthenticationDetails(authResult.getUsername(), authResult.getTicket(),null));
		}catch(AuthenticationException e){
			System.out.println("EXCEPTION CLASS:"+e.getClass());
			RemoteException authFault =new RemoteException(e.getMessage(),e);
			throw authFault; 
		}
		catch(Exception e){
			System.out.println("EXCEPTION CLASS:"+e.getClass());
			RemoteException authFault =new RemoteException(e.getMessage(),e);
			
			throw authFault; 
		}
		
		
			
    }
    
    
    /**
     * Start a session
     * used to connect the php part with the java part. the php part is the master which starts the session with username and password
     * @param username
     * @param ticket
     * @param session
     * @throws RemoteException
     */
    public static void startSession(String username, String ticket, String session)
        throws RemoteException
    {
    	AuthenticationUtils.setAuthenticationDetails(new AuthenticationDetails(username, ticket, session));
    }
    
    
    /**
     * Start a session with cc authentication webservice Auth Method CAS
     * @param username
     * @param proxyTicket
     * @throws RemoteException
     */
    public static void startSessionByCAS(String username, String proxyTicket) throws RemoteException{
    	String ccAuthWsUrl = ApplicationInfoList.getHomeRepository().getAuthenticationwebservice();
    	AuthenticationServiceLocator locator = new AuthenticationServiceLocator();
    	locator.setauthenticationEndpointAddress(ccAuthWsUrl);
    	try{
			AuthenticationSoapBindingStub stub = (AuthenticationSoapBindingStub)locator.getPort(Authentication.class);
			AuthenticationResult authResult = stub.authenticateByCAS(username, proxyTicket);
			
			AuthenticationUtils.setAuthenticationDetails(new AuthenticationDetails(authResult.getUsername(), authResult.getTicket(), authResult.getSessionid()));
    	}catch(Exception e){
    		e.printStackTrace();
    		RemoteException authFault = new RemoteException(e.getMessage());
			throw authFault; 
    	}
    	
    }
    
    /**
     * cc extension
     * @param username
     * @param ticket
     * @param appInfo
     * @return
     * @throws RemoteException
     * @throws ServiceException
     */
    public static boolean checkTicket(String username,String ticket, ApplicationInfo appInfo) throws RemoteException, ServiceException{
    	String ccAuthWsUrl = appInfo.getAuthenticationwebservice();
    	AuthenticationServiceLocator locator = new AuthenticationServiceLocator();
    	locator.setauthenticationEndpointAddress(ccAuthWsUrl);
    	boolean result = false;
    	try{
			AuthenticationSoapBindingStub stub = (AuthenticationSoapBindingStub)locator.getPort(Authentication.class);
			result = stub.checkTicket(username, ticket);
			//TODO org.edu_sharing.ws.authentication.AuthenticationException should not be thrown
    	}catch(Exception e){
    
			throw new RemoteException(e.getMessage(),e); 
    	}
    	return result;
    }
    
	
}
