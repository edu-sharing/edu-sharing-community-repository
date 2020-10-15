package org.edu_sharing.service.connector;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;

public class ConnectorService {
	
	public static final String ID_ONYX = "ONYX";
	
	public static final String ID_ONLY_OFFICE = "ONLY_OFFICE";
	
	public static final String ID_TINYMCE = "TINYMCE";

	ConnectorList connectorList;

	public ConnectorService(){
		Config config= LightbendConfigLoader.get().getConfig("connectorList");
		connectorList=ConfigBeanFactory.create(config,ConnectorList.class);
	}
	
}
