package org.edu_sharing.alfresco.service.connector;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;

import java.io.Serializable;

public class ConnectorService implements Serializable {
	
	public static final String ID_ONYX = "ONYX";
	
	public static final String ID_ONLY_OFFICE = "ONLY_OFFICE";
	
	public static final String ID_TINYMCE = "TINYMCE";

	ConnectorList connectorList;

	public ConnectorService(){
		Config config= LightbendConfigLoader.get().getConfig("connectorList");
		connectorList=ConfigBeanFactory.create(config,ConnectorList.class);
	}

	public ConnectorList getConnectorList() {
		return connectorList;
	}
}
