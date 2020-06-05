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

public class CheckForDuplicatesResult implements Serializable {

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
