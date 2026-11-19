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

	/** as stored, null for shares created before view/download could be chosen, see {@link #isViewAllowed()} */
	private Boolean view;
	/** as stored, null for shares created before view/download could be chosen, see {@link #isDownloadAllowed()} */
	private Boolean download;
	/** context default for a share that has no stored {@link #view}, not persisted */
	private Boolean defaultView;
	/** context default for a share that has no stored {@link #download}, not persisted */
	private Boolean defaultDownload;

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

	/**
	 * Raw value as stored, {@code null} if the share was created before view/download could be chosen.
	 */
	public Boolean getView() {
		return view;
	}

	public void setView(Boolean view) {
		this.view = view;
	}

	public Boolean getDownload() {
		return download;
	}

	public void setDownload(Boolean download) {
		this.download = download;
	}

	public void setDefaultView(Boolean defaultView) {
		this.defaultView = defaultView;
	}

	public void setDefaultDownload(Boolean defaultDownload) {
		this.defaultDownload = defaultDownload;
	}

	/**
	 * Whether the recipient may view the content in the browser. A share without stored value uses the
	 * context default; without a known default it does not allow it.
	 */
	public boolean isViewAllowed() {
		if (view != null) {
			return view;
		}
		return Boolean.TRUE.equals(defaultView);
	}

	/**
	 * Whether the recipient may download the content. A share without stored value uses the context
	 * default; without a known default it allows it, as before view/download could be chosen.
	 */
	public boolean isDownloadAllowed() {
		if (download != null) {
			return download;
		}
		return defaultDownload == null || defaultDownload;
	}
}
