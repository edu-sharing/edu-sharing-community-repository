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
package org.edu_sharing.webservices.authentication;

import java.net.URL;
import java.rmi.RemoteException;

import javax.xml.rpc.ServiceException;

import org.apache.axis.client.Stub;
import org.apache.axis.message.SOAPHeaderElement;
import org.apache.commons.codec.binary.Base64;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.security.Signing;
import org.edu_sharing.webservices.render.RenderInfo;
import org.edu_sharing.webservices.render.RenderInfoServiceLocator;
import org.edu_sharing.webservices.types.KeyValue;

public class Test {
	public static void main(String[] args) {
		AuthenticationServiceLocator locator = new AuthenticationServiceLocator();
		
		locator.setauthenticationEndpointAddress("http://localhost:8080/edu-sharing/services/authentication");
		try{
			Authentication auth = locator.getauthentication();
			AuthenticationResult ar = auth.authenticate("usr", "pw");
			System.out.println(ar.getUsername());

			if (true) { return; }
		
			boolean result = auth.checkTicket("admin", "TICKET_26c7b28e7548095f2cb5aefb7957394c5f0825e6");
			System.out.println("result:"+result);
			
			AuthenticationResult authResult = auth.authenticate("hupfer", "test");
			System.out.println("new ticket:" + authResult.getTicket());
			
		}catch(ServiceException e){
			e.printStackTrace();
		}catch( AuthenticationException e){
			
			//152
			System.out.println(e.getMessage());
			System.out.println(e.getMessage1());
			
		}catch(org.apache.axis.AxisFault e){
			//stable demo
			System.out.println(e.getCause());
			System.out.println(e.getMessage());
		}catch(RemoteException e){
			
			System.out.println(""+e.getClass().getName());
			
			e.printStackTrace();
		}catch(Exception e){
			e.printStackTrace();
		}
		
	}
	
	

}
