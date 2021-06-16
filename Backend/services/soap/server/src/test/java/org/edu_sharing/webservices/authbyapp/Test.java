package org.edu_sharing.webservices.authbyapp;

import java.net.URLEncoder;
import java.security.GeneralSecurityException;

import org.apache.axis.client.Stub;
import org.apache.axis.message.SOAPHeaderElement;
import org.apache.commons.codec.binary.Base64;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.security.Signing;
import org.edu_sharing.webservices.authentication.AuthenticationResult;
import org.edu_sharing.webservices.authentication.AuthenticationServiceLocator;
import org.edu_sharing.webservices.types.KeyValue;

public class Test {

	public static void main(String[] args) {
		
		new Test().testSigned();
		
		if(true) return;
		AuthByAppServiceLocator locator = new AuthByAppServiceLocator();
		
		
		locator.setauthbyappEndpointAddress("http://localhost:8081/edu-sharing/services/authbyapp");
		
		//((Stub)stub).setHeader(new SOAPHeaderElement("http://webservices.edu_sharing.org","signature",new String(signature)));
		//((Stub)stub).setHeader(new SOAPHeaderElement("http://webservices.edu_sharing.org","signed",signData));
		
		try{
			AuthByApp stub = locator.getauthbyapp();
			((Stub)stub).setHeader(new SOAPHeaderElement("http://webservices.edu_sharing.org","appId","moodle_584815cc94cc5"));
			
			String signed = "12233";
			Signing sigTool = new Signing();
			
			ApplicationInfo homeRep = new ApplicationInfo("homeApplication.properties.xml");
			String privateKey = homeRep.getPrivateKey();
			
			try{
				if(privateKey != null){
					
					String timestamp = new Long(System.currentTimeMillis()).toString();
					signed = signed + timestamp;
					byte[] signature = sigTool.sign(sigTool.getPemPrivateKey(privateKey, CCConstants.SECURITY_KEY_ALGORITHM), signed, CCConstants.SECURITY_SIGN_ALGORITHM);
						
					String b64 = java.util.Base64.getEncoder().encodeToString(signature);
					String urlSig = URLEncoder.encode(java.util.Base64.getEncoder().encodeToString(signature));
					//String urlSig = URLEncoder.encode(new Base64().encodeToString(signature));
					
					((Stub)stub).setHeader(new SOAPHeaderElement("http://webservices.edu_sharing.org","signature",b64));
					((Stub)stub).setHeader(new SOAPHeaderElement("http://webservices.edu_sharing.org","signed",signed));
					((Stub)stub).setHeader(new SOAPHeaderElement("http://webservices.edu_sharing.org","timestamp",timestamp));
																									 
				}
			}catch(GeneralSecurityException e){
				e.printStackTrace();
				
			}
			
		
			System.out.println("Sending request");
			AuthenticationResult result = stub.authenticateByTrustedApp("moodle_584815cc94cc5",new KeyValue[]{new KeyValue("userid","admin"),
					new KeyValue("userid","admin"),
					new KeyValue("globalgroups","[{\"id\":\"11\",\"contextid\":\"1\",\"name\":\"EduSharing Gruppe\",\"idnumber\":\"eduTest\"}]"),
					new KeyValue("clientId","eduApp"),
					new KeyValue("clientSecret","secret"),
					new KeyValue("userid","admin")});
			System.out.println(
			"result:"+result.getGivenname() +" "+
			result.getSurname()+" "+
			result.getUsername()+" "+
			result.getTicket()
			);
			
		}catch(Exception e){
			e.printStackTrace();
		}
		
		
		
	}
	
	
	public void testSigned() {
		
		try {
			AuthByAppServiceLocator locator = new AuthByAppServiceLocator();
			
			//locator.setauthenticationEndpointAddress("http://intranet.gfaw.edu-sharing.com:8080/edu-sharing/services/authentication");
			
			locator.setauthbyappEndpointAddress("http://localhost:8080/edu-sharing/services/authbyapp");
			
			
			
			String timestamp = ""+System.currentTimeMillis();
			
			String signData = "1234"+timestamp;
			
			Signing signing = new Signing();
			
			String algorithm = "SHA1withRSA";
			
			
			
			ApplicationInfo homeRep = new ApplicationInfo("homeApplication.properties.xml");
			
			System.out.println(homeRep.getPrivateKey());
			
			String privateKey = homeRep.getPrivateKey();
			
			
				
				
			
			byte[] signedBytes = signing.sign(signing.getPemPrivateKey(privateKey,CCConstants.SECURITY_KEY_ALGORITHM), signData, algorithm);
			
			signedBytes = new Base64().encode(signedBytes);
			
			
			Stub stub = (Stub)locator.getauthbyapp();
			//new Date();
			
			stub.setHeader(new SOAPHeaderElement("http://webservices.edu_sharing.org","timestamp",timestamp));
			stub.setHeader(new SOAPHeaderElement("http://webservices.edu_sharing.org","appId","local"));
			stub.setHeader(new SOAPHeaderElement("http://webservices.edu_sharing.org","signature",new String(signedBytes)));
			stub.setHeader(new SOAPHeaderElement("http://webservices.edu_sharing.org","signed",signData));
			stub.setHeader(new SOAPHeaderElement("http://webservices.edu_sharing.org","locale","de_DE"));
		
		
			
			AuthenticationResult result = ((AuthByApp)stub).authenticateByTrustedApp("local",
					new KeyValue[]{new KeyValue("userid","testLMSABA"),
					new KeyValue("globalgroups","[{\"id\":\"11\",\"contextid\":\"1\",\"name\":\"EduSharing Gruppe\",\"idnumber\":\"eduTest\"}]"),
					new KeyValue("affiliation","testLMSGr"),
					new KeyValue("affiliationname","testLMSGrName"),
					new KeyValue("firstname","Erhard"),
					new KeyValue("lastname","Heinz")});
			System.out.println(
			"result:"+result.getGivenname() +" "+
			result.getSurname()+" "+
			result.getUsername()+" "+
			result.getTicket()
			);
				
		}catch(Exception e) {
			e.printStackTrace();
		}
	}

}
