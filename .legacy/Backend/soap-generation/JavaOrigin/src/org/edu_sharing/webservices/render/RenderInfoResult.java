package org.edu_sharing.webservices.render;

import java.util.List;

import org.edu_sharing.webservices.types.Child;
import org.edu_sharing.webservices.types.KeyValue;
import org.edu_sharing.webservices.usage.UsageResult;

public class RenderInfoResult {

	/**
	 * 
Permissions(nur wegen gast?), Usage(nur wegen xml param), Version,modifiedDate (Content),technicalSize?

	 */
	
	
	Boolean guestReadAllowed;
	
	Boolean publishRight;
	
	Boolean userReadAllowed;
	
	Boolean hasContentLicense;

	Boolean isDirectory;

	UsageResult usage;
	
	int contentHash;
	
	KeyValue[] properties;
	
	KeyValue[] propertiesToolInstance;
	
	KeyValue[] labels;
	
	Child[] children;

	String iconUrl;

	String previewUrl;
	
	String mimeTypeUrl;
	
	String[] aspects;
	
	String mdsTemplate;
	
	private java.lang.String[] permissions;
	
	String eduSchoolPrimaryAffiliation;

	String[] remoteRoles;

	public Boolean getGuestReadAllowed() {
		return guestReadAllowed;
	}

	public void setGuestReadAllowed(Boolean guestReadAllowed) {
		this.guestReadAllowed = guestReadAllowed;
	}

	public Boolean getPublishRight() {
		return publishRight;
	}

	public void setPublishRight(Boolean publishRight) {
		this.publishRight = publishRight;
	}

	public Boolean getUserReadAllowed() {
		return userReadAllowed;
	}

	public void setUserReadAllowed(Boolean userReadAllowed) {
		this.userReadAllowed = userReadAllowed;
	}

	public UsageResult getUsage() {
		return usage;
	}

	public void setUsage(UsageResult usage) {
		this.usage = usage;
	}


	public int getContentHash() {
		return contentHash;
	}

	public void setContentHash(int contentHash) {
		this.contentHash = contentHash;
	}

	
	public void setProperties(KeyValue[] properties) {
		this.properties = properties;
	}
	
	public KeyValue[] getProperties() {
		return properties;
	}
	
	public void setLabels(KeyValue[] labels) {
		this.labels = labels;
	}
	
	public KeyValue[] getLabels() {
		return labels;
	}
	
	
	public void setAspects(String[] aspects) {
		this.aspects = aspects;
	}
	
	public String[] getAspects() {
		return aspects;
	}
	
	public void setMimeTypeUrl(String mimeTypeUrl) {
		this.mimeTypeUrl = mimeTypeUrl;
	}
	
	public String getMimeTypeUrl() {
		return mimeTypeUrl;
	}
	
	public void setPreviewUrl(String previewUrl) {
		this.previewUrl = previewUrl;
	}
	
	public String getPreviewUrl() {
		return previewUrl;
	}
	
	public void setmdsTemplate(String mdsTemplate) {
		this.mdsTemplate = mdsTemplate;
	}
	
	public String getMdsTemplate() {
		return mdsTemplate;
	}
	
	/**
     * Gets the permissions value for this RenderInfoResult.
     * 
     * @return permissions
     */
    public java.lang.String[] getPermissions() {
        return permissions;
    }
    
    /**
     * Sets the permissions value for this RenderInfoResult.
     * 
     * @param permissions
     */
    public void setPermissions(java.lang.String[] permissions) {
        this.permissions = permissions;
    }
    
    /**
     * Sets the propertiesToolInstance value for this RenderInfoResult.
     * 
     * @param propertiesToolInstance
     */
    public void setPropertiesToolInstance(org.edu_sharing.webservices.types.KeyValue[] propertiesToolInstance) {
        this.propertiesToolInstance = propertiesToolInstance;
    }
    
    /**
     * Gets the propertiesToolInstance value for this RenderInfoResult.
     * 
     * @return propertiesToolInstance
     */
    public org.edu_sharing.webservices.types.KeyValue[] getPropertiesToolInstance() {
        return propertiesToolInstance;
    }
    
    public void setHasContentLicense(Boolean hasContentLicense) {
		this.hasContentLicense = hasContentLicense;
	}
    public Boolean getHasContentLicense() {
		return hasContentLicense;
	}
    
    /**
     * Sets the mdsTemplate value for this RenderInfoResult.
     * 
     * @param mdsTemplate
     */
    public void setMdsTemplate(java.lang.String mdsTemplate) {
        this.mdsTemplate = mdsTemplate;
    }
    
    public void setEduSchoolPrimaryAffiliation(String eduSchoolPrimaryAffiliation) {
		this.eduSchoolPrimaryAffiliation = eduSchoolPrimaryAffiliation;
	}
    
    public String getEduSchoolPrimaryAffiliation() {
		return eduSchoolPrimaryAffiliation;
	}

	public Child[] getChildren() {
		return children;
	}

	public void setChildren(Child[] children) {
		this.children = children;
	}

	public String getIconUrl() {
		return iconUrl;
	}

	public void setIconUrl(String iconUrl) {
		this.iconUrl = iconUrl;
	}

	public Boolean getDirectory() {
		return isDirectory;
	}

	public void setDirectory(Boolean directory) {
		isDirectory = directory;
	}

    public void setRemoteRoles(String[] remoteRoles) {
		this.remoteRoles = remoteRoles;
	}

    public String[] getRemoteRoles() {
		return remoteRoles;
	}


}
