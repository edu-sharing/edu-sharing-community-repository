package org.edu_sharing.repository.client.rpc;

import java.io.Serializable;
import java.util.Date;


public class Notify implements Serializable {

	
	String notifyUser;
	String notifyEvent;
	String notifyAction;
	String notifyTarget;
	String nodeId;
	
	
	Date created;
	String createdFormated;
	ACL acl;
	
	String change;
	String currentState;
	
	public String getNodeId() {
		return nodeId;
	}

	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}

	public String getNotifyUser() {
		return notifyUser;
	}

	public void setNotifyUser(String notifyUser) {
		this.notifyUser = notifyUser;
	}

	public String getNotifyEvent() {
		return notifyEvent;
	}

	public void setNotifyEvent(String notifyEvent) {
		this.notifyEvent = notifyEvent;
	}

	public String getNotifyAction() {
		return notifyAction;
	}

	public void setNotifyAction(String notifyAction) {
		this.notifyAction = notifyAction;
	}

	

	public Date getCreated() {
		return created;
	}

	public void setCreated(Date created) {
		this.created = created;
	}
	
	public void setCreatedFormated(String createdFormated) {
		this.createdFormated = createdFormated;
	}
	
	public String getCreatedFormated() {
		return createdFormated;
	}

	public ACL getAcl() {
		return acl;
	}

	public void setAcl(ACL acl) {
		this.acl = acl;
	}

	public String getNotifyTarget() {
		return notifyTarget;
	}

	public void setNotifyTarget(String notifyTarget) {
		this.notifyTarget = notifyTarget;
	}
	
	
	public void setChange(String change) {
		this.change = change;
	}
	
	public String getChange() {
		return change;
	}
	
	public void setCurrentState(String currentState) {
		this.currentState = currentState;
	}
	
	public String getCurrentState() {
		return currentState;
	}
	
	
	
}
