package org.edu_sharing.service.connector;

public class ConnectorServiceFactory {
	
	static ConnectorService cs = new ConnectorService(); 
	
	public static ConnectorService getConnectorService(){
		return cs;
	}
	
}
