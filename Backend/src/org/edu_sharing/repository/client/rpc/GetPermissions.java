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

public class GetPermissions implements Serializable {
	
	
	ACL permissions = null;
	
	
	AssignedLicense[] assignedLicenses = null;
	
	public GetPermissions() {
	}
	
	public GetPermissions(ACL _permissions, AssignedLicense[] _assignedLicenses) {
		permissions = _permissions;
		assignedLicenses = _assignedLicenses;
	}
	
	/**
	 * @return the permissions
	 */
	public ACL getPermissions() {
		return permissions;
	}

	/**
	 * @param permissions the permissions to set
	 */
	public void setPermissions(ACL permissions) {
		this.permissions = permissions;
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
