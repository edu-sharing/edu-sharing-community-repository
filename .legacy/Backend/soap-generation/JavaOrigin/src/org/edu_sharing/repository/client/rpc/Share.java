package org.edu_sharing.repository.client.rpc;

import java.io.Serializable;
import java.util.Date;

public class Share implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	
	String ioNodeId;
	String nodeId;
	String email;
	String token;
	Long expiryDate;
	
	String expiryDateFormated;
	
	String invitedBy;
	Date invitedAt;
	String invitedAtFormated;
	
	String locale;
	
	public Share() {
		
	}
	
	public Share(String ioNodeId, String nodeId, String email, Long expiryDate, String token, String invitedBy, Date invitedAt) {
		this();
		this.ioNodeId = ioNodeId;
		this.nodeId = nodeId;
		this.email = email;
		this.setExpiryDate(expiryDate);
		this.setInvitedAt(invitedAt);
		this.invitedBy = invitedBy;
		this.token = token;
		
	}
	
	
	
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public Long getExpiryDate() {
		return expiryDate;
	}
	public void setExpiryDate(Long expiryDate) {
		this.expiryDate = expiryDate;
	}
	public String getInvitedBy() {
		return invitedBy;
	}
	public void setInvitedBy(String invitedBy) {
		this.invitedBy = invitedBy;
	}
	public Date getInvitedAt() {
		return invitedAt;
	}
	public void setInvitedAt(Date invitedAt) {
		this.invitedAt = invitedAt;
	}
	
	public String getIoNodeId() {
		return ioNodeId;
	}
	
	public String getNodeId() {
		return nodeId;
	}
	
	public void setIoNodeId(String ioNodeId) {
		this.ioNodeId = ioNodeId;
	}
	
	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}
	
	public void setToken(String token) {
		this.token = token;
	}
	
	public String getToken() {
		return token;
	}
	
	public String getExpiryDateFormated() {
		return expiryDateFormated;
	}
	
	public String getInvitedAtFormated() {
		return invitedAtFormated;
	}
	
	public void setExpiryDateFormated(String expiryDateFormated) {
		this.expiryDateFormated = expiryDateFormated;
	}
	
	public void setInvitedAtFormated(String invitedAtFormated) {
		this.invitedAtFormated = invitedAtFormated;
	}

}
