package org.edu_sharing.service.connector;

import org.alfresco.repo.cache.SimpleCache;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.service.config.model.Config;
import org.edu_sharing.service.toolpermission.ToolPermissionService;
import org.edu_sharing.service.toolpermission.ToolPermissionServiceFactory;
import org.edu_sharing.spring.ApplicationContextFactory;

import java.util.ArrayList;
import java.util.List;

public class ConnectorServiceFactory {

	private static String KEY_DEFAULT = "DEFAULT";

	private static SimpleCache<String, ConnectorService> connectorServiceCache = (SimpleCache<String, ConnectorService>) AlfAppContextGate.getApplicationContext().getBean("eduSharingConnectorServiceCache");
	//static ConnectorService cs = new ConnectorService();
	
	public static ConnectorService getConnectorService(){
		return connectorServiceCache.get(KEY_DEFAULT);
	}
	public static void invalidate(){
		connectorServiceCache.clear();connectorServiceCache.put(KEY_DEFAULT,new ConnectorService());
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
