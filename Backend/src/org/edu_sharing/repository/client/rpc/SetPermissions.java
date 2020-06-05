/**
 *
 *  
 * 
 * 
 *	
 *
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 *
 */
package org.edu_sharing.repository.client.rpc;

import java.io.Serializable;
import java.util.ArrayList;

public class SetPermissions implements Serializable {

	ArrayList<PermissionContainer> permissionContainers = null;
	
	String nodeId = null;
	
	Boolean inherit = false;
	
	AssignedLicense[] assignedLicenses = null;
	
	public SetPermissions() {
	}
	
	public SetPermissions(String _nodeId, Boolean _inherit, ArrayList<PermissionContainer> _permissionContainers,AssignedLicense[] _assignedLicenses){
		nodeId = _nodeId;
		inherit = _inherit;
		permissionContainers = _permissionContainers;
		assignedLicenses = _assignedLicenses;
	}

	/**
	 * @return the permissionContainers
	 */
	public ArrayList<PermissionContainer> getPermissionContainers() {
		return permissionContainers;
	}

	/**
	 * @param permissionContainers the permissionContainers to set
	 */
	public void setPermissionContainers(
			ArrayList<PermissionContainer> permissionContainers) {
		this.permissionContainers = permissionContainers;
	}

	/**
	 * @return the nodeId
	 */
	public String getNodeId() {
		return nodeId;
	}

	/**
	 * @param nodeId the nodeId to set
	 */
	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}

	/**
	 * @return the inherited
	 */
	public Boolean getInherit() {
		return inherit;
	}

	/**
	 * @param inherited the inherited to set
	 */
	public void setInherit(Boolean _inherit) {
		this.inherit = _inherit;
	}
	
	/**
	 * @return the assignedLicenses
	 */
	public AssignedLicense[] getAssignedLicenses() {
		return assignedLicenses;
	}

	/**
	 * @param assignedLicenses the assignedLicenses to set
	 */
	public void setAssignedLicenses(AssignedLicense[] assignedLicenses) {
		this.assignedLicenses = assignedLicenses;
	}
	
}
