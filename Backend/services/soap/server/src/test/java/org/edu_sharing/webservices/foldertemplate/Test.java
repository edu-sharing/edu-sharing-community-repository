package org.edu_sharing.webservices.foldertemplate;

import java.net.URL;

import org.edu_sharing.webservices.alfresco.extension.NativeAlfrescoWrapper;
import org.edu_sharing.webservices.alfresco.extension.NativeAlfrescoWrapperServiceLocator;
import org.edu_sharing.webservices.authentication.Authentication;
import org.edu_sharing.webservices.authentication.AuthenticationResult;
import org.edu_sharing.webservices.util.AuthenticationDetails;
import org.edu_sharing.webservices.util.AuthenticationUtils;
import org.edu_sharing.webservices.util.EduWebServiceFactory;

public class Test {

	public static void main(String[] args) {
		try{
			
			Authentication auth = EduWebServiceFactory.getAuthenticationServiceByEndpointAddress("http://127.0.0.1:8080/edu-sharing/services/authentication");
			AuthenticationResult authResult =  auth.authenticate("usr", "pw");
			
			AuthenticationUtils.setAuthenticationDetails(new AuthenticationDetails(authResult.getUsername(), authResult.getTicket(), authResult.getSessionid()));
			
			FolderTemplate ft = new FolderTemplateServiceLocator(AuthenticationUtils.getEngineConfiguration()).getFolderTemplate(new URL("http://127.0.0.1:8080/edu-sharing/services/FolderTemplate"));
			ft.process("template.xml", "GROUP_edugroup1", null);
		
		}catch(Throwable e){
			e.printStackTrace();
		}

	}

}
