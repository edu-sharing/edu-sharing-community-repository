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
	int downloadCount;
	
	String expiryDateFormated;
	
	String invitedBy;
	
	String invitedByCaption;
	
	Date invitedAt;
	String invitedAtFormated;
	
	String locale;
	private String password;

	public Share() {
		
	}
	
	public Share(String ioNodeId, String nodeId, String email, Long expiryDate, String token, String invitedBy, Date invitedAt, int downloadCount) {
		this();
		this.ioNodeId = ioNodeId;
		this.nodeId = nodeId;
		this.email = email;
		this.setExpiryDate(expiryDate);
		this.setInvitedAt(invitedAt);
		this.invitedBy = invitedBy;
		this.token = token;
		this.downloadCount = downloadCount;
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
	public String getInvitedByCaption() {
		return invitedByCaption;
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
	
	public Integer getDownloadCount() {
		return downloadCount;
	}
	
	public void setDownloadCount(Integer downloadCount) {
		this.downloadCount = downloadCount;
	}
	
	public void setInvitedByCaption(String invitedByCaption) {
		this.invitedByCaption = invitedByCaption;
	}

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }
}
