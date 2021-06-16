package org.edu_sharing.repository.client.rpc;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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

	Date invitedAt;

	private String password;
	private Map<String, Object> properties;

	public Share() {}
	
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

	public Integer getDownloadCount() {
		return downloadCount;
	}
	
	public void setDownloadCount(Integer downloadCount) {
		this.downloadCount = downloadCount;
	}

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

	public void setProperties(Map<String, Object> properties) {
		this.properties = properties;
	}

	public Map<String, Object> getProperties() {
		return properties;
	}
}
