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

public class CheckForDuplicatesResult implements com.google.gwt.user.client.rpc.IsSerializable {

	boolean nodeExists = false;
	
	String recommendValue = null;
	
	public CheckForDuplicatesResult() {
	}

	public boolean isNodeExists() {
		return nodeExists;
	}

	public void setNodeExists(boolean nodeExists) {
		this.nodeExists = nodeExists;
	}

	public String getRecommendValue() {
		return recommendValue;
	}

	public void setRecommendValue(String recommendValue) {
		this.recommendValue = recommendValue;
	}
}
