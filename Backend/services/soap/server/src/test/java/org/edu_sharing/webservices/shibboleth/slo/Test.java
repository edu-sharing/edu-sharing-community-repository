package org.edu_sharing.webservices.shibboleth.slo;

import java.net.URL;

public class Test {

	public static void main(String[] args) {
		try{
			LogoutNotification ln = new LogoutNotificationServiceLocator().getLogoutNotification(new URL("https://127.0.0.1/edu-sharing/services/LogoutNotification?wsdl"));
			ln.logoutNotification(new String[]{"123Test"});
		}catch(Exception e){
			e.printStackTrace();
		}
	}

}
