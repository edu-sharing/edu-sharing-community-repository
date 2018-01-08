package org.edu_sharing.restservices.shared;

import org.edu_sharing.repository.client.rpc.User;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;


@ApiModel(description = "")
public class UserStats  {
  
	private int nodeCount = 0;
	private int nodeCountCC = 0;
	private int collectionCount = 0;
	
	public int getNodeCount() {
		return nodeCount;
	}
	public void setNodeCount(int nodeCount) {
		this.nodeCount = nodeCount;
	}
	public int getCollectionCount() {
		return collectionCount;
	}
	public void setCollectionCount(int collectionCount) {
		this.collectionCount = collectionCount;
	}
	public int getNodeCountCC() {
		return nodeCountCC;
	}
	public void setNodeCountCC(int nodeCountCC) {
		this.nodeCountCC = nodeCountCC;
	}
	
	  
}
