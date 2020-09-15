package org.edu_sharing.service.connector;

import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.service.toolpermission.ToolPermissionService;
import org.edu_sharing.service.toolpermission.ToolPermissionServiceFactory;
import org.edu_sharing.spring.ApplicationContextFactory;

import java.util.ArrayList;
import java.util.List;

public class ConnectorServiceFactory {
	
	static ConnectorService cs = new ConnectorService(); 
	
	public static ConnectorService getConnectorService(){
		return cs;
	}
	public static void invalidate(){
		cs = new ConnectorService();
	}

	public static ConnectorList getConnectorList(){
		return getConnectorList(ToolPermissionServiceFactory.getInstance());
	}
	public static ConnectorList getConnectorList(ToolPermissionService toolPermissionService){

		try{
			if(new MCAlfrescoAPIClient().isAdmin()){
				return getConnectorService().connectorList;
			}
		}catch(Exception e){

		}

		ConnectorList filteredList = new ConnectorList();

		List<Connector> filteredConnectors = new ArrayList<Connector>();
		for(Connector connector : getConnectorService().connectorList.getConnectors()){
			if(toolPermissionService.hasToolPermissionForConnector(connector.getId())){
				filteredConnectors.add(connector);
			}
		}
		filteredList.setConnectors(filteredConnectors);
		return filteredList;
	}
}
