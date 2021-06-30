package org.edu_sharing.service.toolpermission;

import java.util.*;

import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.service.toolpermission.ToolPermissionBaseService;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.alfresco.repository.server.authentication.Context;
import org.edu_sharing.alfresco.service.connector.Connector;
import org.edu_sharing.alfresco.service.connector.ConnectorList;
import org.edu_sharing.service.connector.ConnectorServiceFactory;

public class ToolPermissionService extends ToolPermissionBaseService {
	private Logger logger = Logger.getLogger(ToolPermissionService.class);
	org.edu_sharing.service.nodeservice.NodeService eduNodeService;


	public void setEduNodeService(org.edu_sharing.service.nodeservice.NodeService eduNodeService) {
		this.eduNodeService = eduNodeService;
	}
	/**
	 * Clears previously stored tool permissions in the current http session, e.g. when user changes
	 */
	public void invalidateSessionCache() {
		try{
			HttpSession session = Context.getCurrentInstance().getRequest().getSession();
			for(String tp : this.getAllToolPermissions(false)){
				session.removeAttribute(tp);
			}
		}catch(Throwable t){
			// may fails when no session is active, not an issue
		}
	}

	public boolean hasToolPermissionForConnector(String connectorId){
		AuthenticationToolAPI authTool = new AuthenticationToolAPI();
		String scope=authTool.getScope();
		if(scope==null)
			scope="";
		else
			scope="_"+scope;
		return hasToolPermission(CCConstants.CCM_VALUE_TOOLPERMISSION_CONNECTOR_PREFIX + connectorId+scope);
	}

	public void init(){
		try {
			initToolPermissions(getAllPredefinedToolPermissions());
		} catch (Throwable throwable) {
			throw new RuntimeException(throwable);
		}
	}

	protected void addConnectorToolpermissions(List<String> toInit) {
		ConnectorList connectorList =  ConnectorServiceFactory.getConnectorList(this);
		for(Connector c : connectorList.getConnectors()){
			String tp = CCConstants.CCM_VALUE_TOOLPERMISSION_CONNECTOR_PREFIX + c.getId();
			toInit.add(tp);

			String tp_safe = tp + "_safe";
			toInit.add(tp_safe);
		}
	}
}
