package org.edu_sharing.service.toolpermission;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.connector.Connector;
import org.edu_sharing.service.connector.ConnectorList;
import org.edu_sharing.service.connector.ConnectorServiceFactory;

public class ToolPermissionServiceFactory {

	static Logger logger = Logger.getLogger(ToolPermissionServiceFactory.class);
	
	static ToolPermissionService tps = null;
	public static List<String> getAllDefaultAllowedToolpermissions(){
		List<String> toInit=getAllPredefinedToolPermissions();
		toInit.remove(CCConstants.CCM_VALUE_TOOLPERMISSION_CONFIDENTAL); // safe
		toInit.remove(CCConstants.CCM_VALUE_TOOLPERMISSION_COLLECTION_EDITORIAL); // editorial collections
		toInit.remove(CCConstants.CCM_VALUE_TOOLPERMISSION_COLLECTION_CURRICULUM); // curriculum collections
		toInit.remove(CCConstants.CCM_VALUE_TOOLPERMISSION_COLLECTION_PINNING); // pin collections
		toInit.remove(CCConstants.CCM_VALUE_TOOLPERMISSION_HANDLESERVICE); // use handle id
		return toInit;
	}
	public static List<String> getAllPredefinedToolPermissions(){
		List<String> toInit=new ArrayList<String>();
		toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH);
		toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH_FUZZY);
		toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH_SHARE);
		toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH_SAFE);
		toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH_SHARE_SAFE);
		
		toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_INVITE);
		toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_INVITE_SHARE);
		toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_INVITE_SAFE);
		toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_INVITE_SHARE_SAFE);
		
		toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_INVITE_ALLAUTHORITIES);
		toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_INVITE_HISTORY);
		toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_LICENSE);
		toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_UNCHECKEDCONTENT);
		toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_WORKSPACE);
		toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_CONFIDENTAL);
		
		toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_COLLECTION_EDITORIAL);
		toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_COLLECTION_CURRICULUM);
		toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_COLLECTION_PINNING);
		toInit.add(CCConstants.CCM_VALUE_TOOLPERMISSION_HANDLESERVICE);

		addConnectorToolpermissions(toInit);
		return toInit;
	}

	private static void addConnectorToolpermissions(List<String> toInit) {
		ConnectorList connectorList =  ConnectorServiceFactory.getConnectorService().getConnectorList();
		for(Connector c : connectorList.getConnectors()){
			String tp = CCConstants.CCM_VALUE_TOOLPERMISSION_CONNECTOR_PREFIX + c.getId();
			toInit.add(tp);

			String tp_safe = tp + "_safe";
			toInit.add(tp_safe);
		}
	}

	public static ToolPermissionService getInstance(){
		if( tps == null){
			 tps = new ToolPermissionService();
			 			
			try{
				tps.initToolPermissions(getAllPredefinedToolPermissions());
			}catch(Throwable e){
				logger.error(e.getMessage(),e);
				return null;
			}
		}
		
		return tps;
	}
	
	
}
