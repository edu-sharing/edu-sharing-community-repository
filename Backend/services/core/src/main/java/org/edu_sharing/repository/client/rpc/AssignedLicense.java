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

public class AssignedLicense implements Serializable {
	
	String nodeId = null;

	String[] licenses = null;
	String authority = null;
	
	public AssignedLicense() {
	}
	
	public AssignedLicense(String _nodeId, String _authority, String[] _licenses) {
		nodeId = _nodeId;
		authority = _authority;
		licenses = _licenses;
	}

	/**
	 * @return the licenses
	 */
	public String[] getLicenses() {
		return licenses;
	}

	/**
	 * @param licenses the licenses to set
	 */
	public void setLicenses(String[] licenses) {
		this.licenses = licenses;
	}

	/**
	 * @return the authority
	 */
	public String getAuthority() {
		return authority;
	}

	/**
	 * @param authority the authority to set
	 */
	public void setAuthority(String authority) {
		this.authority = authority;
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
}
