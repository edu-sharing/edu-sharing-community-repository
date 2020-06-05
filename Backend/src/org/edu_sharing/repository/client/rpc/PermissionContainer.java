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
import java.util.HashMap;

public class PermissionContainer implements Serializable {
	
	
	
	String authorityType = null;
	String authorityName = null;
	HashMap authorityProps = null;
	
	String[] permissionsToSet = null;
	
	String[] permissionsToRemove = null;
	
	
	
	public PermissionContainer(){
		
	}
	
	
	/**
	 * @return the permissionsToSet
	 */
	public String[] getPermissionsToSet() {
		return permissionsToSet;
	}


	/**
	 * @param permissionsToSet the permissionsToSet to set
	 */
	public void setPermissionsToSet(String[] permissionsToSet) {
		this.permissionsToSet = permissionsToSet;
	}


	/**
	 * @return the permissionsToRemove
	 */
	public String[] getPermissionsToRemove() {
		return permissionsToRemove;
	}


	/**
	 * @param permissionsToRemove the permissionsToRemove to set
	 */
	public void setPermissionsToRemove(String[] permissionsToRemove) {
		this.permissionsToRemove = permissionsToRemove;
	}


	/**
	 * @return the authorityType
	 */
	public String getAuthorityType() {
		return authorityType;
	}
	/**
	 * @param authorityType the authorityType to set
	 */
	public void setAuthorityType(String authorityType) {
		this.authorityType = authorityType;
	}
	/**
	 * @return the authorityName
	 */
	public String getAuthorityName() {
		return authorityName;
	}
	/**
	 * @param authorityName the authorityName to set
	 */
	public void setAuthorityName(String authorityName) {
		this.authorityName = authorityName;
	}
	/**
	 * @return the authorityProps
	 */
	public HashMap getAuthorityProps() {
		return authorityProps;
	}
	/**
	 * @param authorityProps the authorityProps to set
	 */
	public void setAuthorityProps(HashMap authorityProps) {
		this.authorityProps = authorityProps;
	}
	
	
}
