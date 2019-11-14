package org.edu_sharing.service.connector;

import java.util.ArrayList;
import java.util.List;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import org.edu_sharing.lightbend.LightbendConfigLoader;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.service.toolpermission.ToolPermissionService;
import org.edu_sharing.service.toolpermission.ToolPermissionServiceFactory;
import org.edu_sharing.spring.ApplicationContextFactory;

public class ConnectorService {
	
	public static final String ID_ONYX = "ONYX";
	
	public static final String ID_ONLY_OFFICE = "ONLY_OFFICE";
	
	public static final String ID_TINYMCE = "TINYMCE";

	ConnectorList connectorList;

	public ConnectorService(){
		Config config=LightbendConfigLoader.get().getConfig("connectorList");
		connectorList=ConfigBeanFactory.create(config,ConnectorList.class);
	}
	
}
