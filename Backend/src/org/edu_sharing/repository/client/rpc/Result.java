package org.edu_sharing.repository.client.rpc;

import com.google.gwt.user.client.rpc.IsSerializable;

public class Result<E> implements IsSerializable {

	
	private int nodeCount = 0;
	// the startIDX can change when the result nr changed between the requests
	// so the right startIDx must be displayed on the client
	private int startIDX = 0;
	
	
	E data;
	
	public int getNodeCount() {
		return nodeCount;
	}
	public void setNodeCount(int nodeCount) {
		this.nodeCount = nodeCount;
	}
	
	
	public int getStartIDX() {
		return startIDX;
	}
	public void setStartIDX(int startIDX) {
		this.startIDX = startIDX;
	}
	
	
	public void setData(E data) {
		this.data = data;
	}
	
	public E getData(){
		return this.data;
	}
	
}
