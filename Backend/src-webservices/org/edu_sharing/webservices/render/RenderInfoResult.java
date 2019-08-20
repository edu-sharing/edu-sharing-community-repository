/**
 * RenderInfoResult.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.render;

public class RenderInfoResult  implements java.io.Serializable {
    private java.lang.String[] aspects;

    private org.edu_sharing.webservices.types.Child[] children;

    private int contentHash;

    private java.lang.Boolean directory;

    private java.lang.String eduSchoolPrimaryAffiliation;

    private java.lang.Boolean guestReadAllowed;

    private java.lang.Boolean hasContentLicense;

    private java.lang.String iconUrl;

    private org.edu_sharing.webservices.types.KeyValue[] labels;

    private java.lang.String mdsTemplate;

    private java.lang.String mimeTypeUrl;

    private java.lang.String[] permissions;

    private java.lang.String previewUrl;

    private org.edu_sharing.webservices.types.KeyValue[] properties;

    private org.edu_sharing.webservices.types.KeyValue[] propertiesToolInstance;

    private java.lang.Boolean publishRight;

    private java.lang.String[] remoteRoles;

    private org.edu_sharing.webservices.usage.UsageResult usage;

    private java.lang.Boolean userReadAllowed;

    public RenderInfoResult() {
    }

    public RenderInfoResult(
           java.lang.String[] aspects,
           org.edu_sharing.webservices.types.Child[] children,
           int contentHash,
           java.lang.Boolean directory,
           java.lang.String eduSchoolPrimaryAffiliation,
           java.lang.Boolean guestReadAllowed,
           java.lang.Boolean hasContentLicense,
           java.lang.String iconUrl,
           org.edu_sharing.webservices.types.KeyValue[] labels,
           java.lang.String mdsTemplate,
           java.lang.String mimeTypeUrl,
           java.lang.String[] permissions,
           java.lang.String previewUrl,
           org.edu_sharing.webservices.types.KeyValue[] properties,
           org.edu_sharing.webservices.types.KeyValue[] propertiesToolInstance,
           java.lang.Boolean publishRight,
           java.lang.String[] remoteRoles,
           org.edu_sharing.webservices.usage.UsageResult usage,
           java.lang.Boolean userReadAllowed) {
           this.aspects = aspects;
           this.children = children;
           this.contentHash = contentHash;
           this.directory = directory;
           this.eduSchoolPrimaryAffiliation = eduSchoolPrimaryAffiliation;
           this.guestReadAllowed = guestReadAllowed;
           this.hasContentLicense = hasContentLicense;
           this.iconUrl = iconUrl;
           this.labels = labels;
           this.mdsTemplate = mdsTemplate;
           this.mimeTypeUrl = mimeTypeUrl;
           this.permissions = permissions;
           this.previewUrl = previewUrl;
           this.properties = properties;
           this.propertiesToolInstance = propertiesToolInstance;
           this.publishRight = publishRight;
           this.remoteRoles = remoteRoles;
           this.usage = usage;
           this.userReadAllowed = userReadAllowed;
    }


    /**
     * Gets the aspects value for this RenderInfoResult.
     * 
     * @return aspects
     */
    public java.lang.String[] getAspects() {
        return aspects;
    }


    /**
     * Sets the aspects value for this RenderInfoResult.
     * 
     * @param aspects
     */
    public void setAspects(java.lang.String[] aspects) {
        this.aspects = aspects;
    }


    /**
     * Gets the children value for this RenderInfoResult.
     * 
     * @return children
     */
    public org.edu_sharing.webservices.types.Child[] getChildren() {
        return children;
    }


    /**
     * Sets the children value for this RenderInfoResult.
     * 
     * @param children
     */
    public void setChildren(org.edu_sharing.webservices.types.Child[] children) {
        this.children = children;
    }


    /**
     * Gets the contentHash value for this RenderInfoResult.
     * 
     * @return contentHash
     */
    public int getContentHash() {
        return contentHash;
    }


    /**
     * Sets the contentHash value for this RenderInfoResult.
     * 
     * @param contentHash
     */
    public void setContentHash(int contentHash) {
        this.contentHash = contentHash;
    }


    /**
     * Gets the directory value for this RenderInfoResult.
     * 
     * @return directory
     */
    public java.lang.Boolean getDirectory() {
        return directory;
    }


    /**
     * Sets the directory value for this RenderInfoResult.
     * 
     * @param directory
     */
    public void setDirectory(java.lang.Boolean directory) {
        this.directory = directory;
    }


    /**
     * Gets the eduSchoolPrimaryAffiliation value for this RenderInfoResult.
     * 
     * @return eduSchoolPrimaryAffiliation
     */
    public java.lang.String getEduSchoolPrimaryAffiliation() {
        return eduSchoolPrimaryAffiliation;
    }


    /**
     * Sets the eduSchoolPrimaryAffiliation value for this RenderInfoResult.
     * 
     * @param eduSchoolPrimaryAffiliation
     */
    public void setEduSchoolPrimaryAffiliation(java.lang.String eduSchoolPrimaryAffiliation) {
        this.eduSchoolPrimaryAffiliation = eduSchoolPrimaryAffiliation;
    }


    /**
     * Gets the guestReadAllowed value for this RenderInfoResult.
     * 
     * @return guestReadAllowed
     */
    public java.lang.Boolean getGuestReadAllowed() {
        return guestReadAllowed;
    }


    /**
     * Sets the guestReadAllowed value for this RenderInfoResult.
     * 
     * @param guestReadAllowed
     */
    public void setGuestReadAllowed(java.lang.Boolean guestReadAllowed) {
        this.guestReadAllowed = guestReadAllowed;
    }


    /**
     * Gets the hasContentLicense value for this RenderInfoResult.
     * 
     * @return hasContentLicense
     */
    public java.lang.Boolean getHasContentLicense() {
        return hasContentLicense;
    }


    /**
     * Sets the hasContentLicense value for this RenderInfoResult.
     * 
     * @param hasContentLicense
     */
    public void setHasContentLicense(java.lang.Boolean hasContentLicense) {
        this.hasContentLicense = hasContentLicense;
    }


    /**
     * Gets the iconUrl value for this RenderInfoResult.
     * 
     * @return iconUrl
     */
    public java.lang.String getIconUrl() {
        return iconUrl;
    }


    /**
     * Sets the iconUrl value for this RenderInfoResult.
     * 
     * @param iconUrl
     */
    public void setIconUrl(java.lang.String iconUrl) {
        this.iconUrl = iconUrl;
    }


    /**
     * Gets the labels value for this RenderInfoResult.
     * 
     * @return labels
     */
    public org.edu_sharing.webservices.types.KeyValue[] getLabels() {
        return labels;
    }


    /**
     * Sets the labels value for this RenderInfoResult.
     * 
     * @param labels
     */
    public void setLabels(org.edu_sharing.webservices.types.KeyValue[] labels) {
        this.labels = labels;
    }


    /**
     * Gets the mdsTemplate value for this RenderInfoResult.
     * 
     * @return mdsTemplate
     */
    public java.lang.String getMdsTemplate() {
        return mdsTemplate;
    }


    /**
     * Sets the mdsTemplate value for this RenderInfoResult.
     * 
     * @param mdsTemplate
     */
    public void setMdsTemplate(java.lang.String mdsTemplate) {
        this.mdsTemplate = mdsTemplate;
    }


    /**
     * Gets the mimeTypeUrl value for this RenderInfoResult.
     * 
     * @return mimeTypeUrl
     */
    public java.lang.String getMimeTypeUrl() {
        return mimeTypeUrl;
    }


    /**
     * Sets the mimeTypeUrl value for this RenderInfoResult.
     * 
     * @param mimeTypeUrl
     */
    public void setMimeTypeUrl(java.lang.String mimeTypeUrl) {
        this.mimeTypeUrl = mimeTypeUrl;
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
     * Gets the previewUrl value for this RenderInfoResult.
     * 
     * @return previewUrl
     */
    public java.lang.String getPreviewUrl() {
        return previewUrl;
    }


    /**
     * Sets the previewUrl value for this RenderInfoResult.
     * 
     * @param previewUrl
     */
    public void setPreviewUrl(java.lang.String previewUrl) {
        this.previewUrl = previewUrl;
    }


    /**
     * Gets the properties value for this RenderInfoResult.
     * 
     * @return properties
     */
    public org.edu_sharing.webservices.types.KeyValue[] getProperties() {
        return properties;
    }


    /**
     * Sets the properties value for this RenderInfoResult.
     * 
     * @param properties
     */
    public void setProperties(org.edu_sharing.webservices.types.KeyValue[] properties) {
        this.properties = properties;
    }


    /**
     * Gets the propertiesToolInstance value for this RenderInfoResult.
     * 
     * @return propertiesToolInstance
     */
    public org.edu_sharing.webservices.types.KeyValue[] getPropertiesToolInstance() {
        return propertiesToolInstance;
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
     * Gets the publishRight value for this RenderInfoResult.
     * 
     * @return publishRight
     */
    public java.lang.Boolean getPublishRight() {
        return publishRight;
    }


    /**
     * Sets the publishRight value for this RenderInfoResult.
     * 
     * @param publishRight
     */
    public void setPublishRight(java.lang.Boolean publishRight) {
        this.publishRight = publishRight;
    }


    /**
     * Gets the remoteRoles value for this RenderInfoResult.
     * 
     * @return remoteRoles
     */
    public java.lang.String[] getRemoteRoles() {
        return remoteRoles;
    }


    /**
     * Sets the remoteRoles value for this RenderInfoResult.
     * 
     * @param remoteRoles
     */
    public void setRemoteRoles(java.lang.String[] remoteRoles) {
        this.remoteRoles = remoteRoles;
    }


    /**
     * Gets the usage value for this RenderInfoResult.
     * 
     * @return usage
     */
    public org.edu_sharing.webservices.usage.UsageResult getUsage() {
        return usage;
    }


    /**
     * Sets the usage value for this RenderInfoResult.
     * 
     * @param usage
     */
    public void setUsage(org.edu_sharing.webservices.usage.UsageResult usage) {
        this.usage = usage;
    }


    /**
     * Gets the userReadAllowed value for this RenderInfoResult.
     * 
     * @return userReadAllowed
     */
    public java.lang.Boolean getUserReadAllowed() {
        return userReadAllowed;
    }


    /**
     * Sets the userReadAllowed value for this RenderInfoResult.
     * 
     * @param userReadAllowed
     */
    public void setUserReadAllowed(java.lang.Boolean userReadAllowed) {
        this.userReadAllowed = userReadAllowed;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof RenderInfoResult)) return false;
        RenderInfoResult other = (RenderInfoResult) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.aspects==null && other.getAspects()==null) || 
             (this.aspects!=null &&
              java.util.Arrays.equals(this.aspects, other.getAspects()))) &&
            ((this.children==null && other.getChildren()==null) || 
             (this.children!=null &&
              java.util.Arrays.equals(this.children, other.getChildren()))) &&
            this.contentHash == other.getContentHash() &&
            ((this.directory==null && other.getDirectory()==null) || 
             (this.directory!=null &&
              this.directory.equals(other.getDirectory()))) &&
            ((this.eduSchoolPrimaryAffiliation==null && other.getEduSchoolPrimaryAffiliation()==null) || 
             (this.eduSchoolPrimaryAffiliation!=null &&
              this.eduSchoolPrimaryAffiliation.equals(other.getEduSchoolPrimaryAffiliation()))) &&
            ((this.guestReadAllowed==null && other.getGuestReadAllowed()==null) || 
             (this.guestReadAllowed!=null &&
              this.guestReadAllowed.equals(other.getGuestReadAllowed()))) &&
            ((this.hasContentLicense==null && other.getHasContentLicense()==null) || 
             (this.hasContentLicense!=null &&
              this.hasContentLicense.equals(other.getHasContentLicense()))) &&
            ((this.iconUrl==null && other.getIconUrl()==null) || 
             (this.iconUrl!=null &&
              this.iconUrl.equals(other.getIconUrl()))) &&
            ((this.labels==null && other.getLabels()==null) || 
             (this.labels!=null &&
              java.util.Arrays.equals(this.labels, other.getLabels()))) &&
            ((this.mdsTemplate==null && other.getMdsTemplate()==null) || 
             (this.mdsTemplate!=null &&
              this.mdsTemplate.equals(other.getMdsTemplate()))) &&
            ((this.mimeTypeUrl==null && other.getMimeTypeUrl()==null) || 
             (this.mimeTypeUrl!=null &&
              this.mimeTypeUrl.equals(other.getMimeTypeUrl()))) &&
            ((this.permissions==null && other.getPermissions()==null) || 
             (this.permissions!=null &&
              java.util.Arrays.equals(this.permissions, other.getPermissions()))) &&
            ((this.previewUrl==null && other.getPreviewUrl()==null) || 
             (this.previewUrl!=null &&
              this.previewUrl.equals(other.getPreviewUrl()))) &&
            ((this.properties==null && other.getProperties()==null) || 
             (this.properties!=null &&
              java.util.Arrays.equals(this.properties, other.getProperties()))) &&
            ((this.propertiesToolInstance==null && other.getPropertiesToolInstance()==null) || 
             (this.propertiesToolInstance!=null &&
              java.util.Arrays.equals(this.propertiesToolInstance, other.getPropertiesToolInstance()))) &&
            ((this.publishRight==null && other.getPublishRight()==null) || 
             (this.publishRight!=null &&
              this.publishRight.equals(other.getPublishRight()))) &&
            ((this.remoteRoles==null && other.getRemoteRoles()==null) || 
             (this.remoteRoles!=null &&
              java.util.Arrays.equals(this.remoteRoles, other.getRemoteRoles()))) &&
            ((this.usage==null && other.getUsage()==null) || 
             (this.usage!=null &&
              this.usage.equals(other.getUsage()))) &&
            ((this.userReadAllowed==null && other.getUserReadAllowed()==null) || 
             (this.userReadAllowed!=null &&
              this.userReadAllowed.equals(other.getUserReadAllowed())));
        __equalsCalc = null;
        return _equals;
    }

    private boolean __hashCodeCalc = false;
    public synchronized int hashCode() {
        if (__hashCodeCalc) {
            return 0;
        }
        __hashCodeCalc = true;
        int _hashCode = 1;
        if (getAspects() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getAspects());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getAspects(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        if (getChildren() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getChildren());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getChildren(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        _hashCode += getContentHash();
        if (getDirectory() != null) {
            _hashCode += getDirectory().hashCode();
        }
        if (getEduSchoolPrimaryAffiliation() != null) {
            _hashCode += getEduSchoolPrimaryAffiliation().hashCode();
        }
        if (getGuestReadAllowed() != null) {
            _hashCode += getGuestReadAllowed().hashCode();
        }
        if (getHasContentLicense() != null) {
            _hashCode += getHasContentLicense().hashCode();
        }
        if (getIconUrl() != null) {
            _hashCode += getIconUrl().hashCode();
        }
        if (getLabels() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getLabels());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getLabels(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        if (getMdsTemplate() != null) {
            _hashCode += getMdsTemplate().hashCode();
        }
        if (getMimeTypeUrl() != null) {
            _hashCode += getMimeTypeUrl().hashCode();
        }
        if (getPermissions() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getPermissions());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getPermissions(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        if (getPreviewUrl() != null) {
            _hashCode += getPreviewUrl().hashCode();
        }
        if (getProperties() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getProperties());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getProperties(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        if (getPropertiesToolInstance() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getPropertiesToolInstance());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getPropertiesToolInstance(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        if (getPublishRight() != null) {
            _hashCode += getPublishRight().hashCode();
        }
        if (getRemoteRoles() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getRemoteRoles());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getRemoteRoles(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        if (getUsage() != null) {
            _hashCode += getUsage().hashCode();
        }
        if (getUserReadAllowed() != null) {
            _hashCode += getUserReadAllowed().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(RenderInfoResult.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://render.webservices.edu_sharing.org", "RenderInfoResult"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("aspects");
        elemField.setXmlName(new javax.xml.namespace.QName("http://render.webservices.edu_sharing.org", "aspects"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        elemField.setItemQName(new javax.xml.namespace.QName("http://render.webservices.edu_sharing.org", "item"));
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("children");
        elemField.setXmlName(new javax.xml.namespace.QName("http://render.webservices.edu_sharing.org", "children"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://types.webservices.edu_sharing.org", "Child"));
        elemField.setNillable(true);
        elemField.setItemQName(new javax.xml.namespace.QName("http://render.webservices.edu_sharing.org", "item"));
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("contentHash");
        elemField.setXmlName(new javax.xml.namespace.QName("http://render.webservices.edu_sharing.org", "contentHash"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("directory");
        elemField.setXmlName(new javax.xml.namespace.QName("http://render.webservices.edu_sharing.org", "directory"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "boolean"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("eduSchoolPrimaryAffiliation");
        elemField.setXmlName(new javax.xml.namespace.QName("http://render.webservices.edu_sharing.org", "eduSchoolPrimaryAffiliation"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("guestReadAllowed");
        elemField.setXmlName(new javax.xml.namespace.QName("http://render.webservices.edu_sharing.org", "guestReadAllowed"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "boolean"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("hasContentLicense");
        elemField.setXmlName(new javax.xml.namespace.QName("http://render.webservices.edu_sharing.org", "hasContentLicense"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "boolean"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("iconUrl");
        elemField.setXmlName(new javax.xml.namespace.QName("http://render.webservices.edu_sharing.org", "iconUrl"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("labels");
        elemField.setXmlName(new javax.xml.namespace.QName("http://render.webservices.edu_sharing.org", "labels"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://types.webservices.edu_sharing.org", "KeyValue"));
        elemField.setNillable(true);
        elemField.setItemQName(new javax.xml.namespace.QName("http://render.webservices.edu_sharing.org", "item"));
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("mdsTemplate");
        elemField.setXmlName(new javax.xml.namespace.QName("http://render.webservices.edu_sharing.org", "mdsTemplate"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("mimeTypeUrl");
        elemField.setXmlName(new javax.xml.namespace.QName("http://render.webservices.edu_sharing.org", "mimeTypeUrl"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("permissions");
        elemField.setXmlName(new javax.xml.namespace.QName("http://render.webservices.edu_sharing.org", "permissions"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        elemField.setItemQName(new javax.xml.namespace.QName("http://render.webservices.edu_sharing.org", "item"));
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("previewUrl");
        elemField.setXmlName(new javax.xml.namespace.QName("http://render.webservices.edu_sharing.org", "previewUrl"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("properties");
        elemField.setXmlName(new javax.xml.namespace.QName("http://render.webservices.edu_sharing.org", "properties"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://types.webservices.edu_sharing.org", "KeyValue"));
        elemField.setNillable(true);
        elemField.setItemQName(new javax.xml.namespace.QName("http://render.webservices.edu_sharing.org", "item"));
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("propertiesToolInstance");
        elemField.setXmlName(new javax.xml.namespace.QName("http://render.webservices.edu_sharing.org", "propertiesToolInstance"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://types.webservices.edu_sharing.org", "KeyValue"));
        elemField.setNillable(true);
        elemField.setItemQName(new javax.xml.namespace.QName("http://render.webservices.edu_sharing.org", "item"));
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("publishRight");
        elemField.setXmlName(new javax.xml.namespace.QName("http://render.webservices.edu_sharing.org", "publishRight"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "boolean"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("remoteRoles");
        elemField.setXmlName(new javax.xml.namespace.QName("http://render.webservices.edu_sharing.org", "remoteRoles"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        elemField.setItemQName(new javax.xml.namespace.QName("http://render.webservices.edu_sharing.org", "item"));
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("usage");
        elemField.setXmlName(new javax.xml.namespace.QName("http://render.webservices.edu_sharing.org", "usage"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://usage.webservices.edu_sharing.org", "UsageResult"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("userReadAllowed");
        elemField.setXmlName(new javax.xml.namespace.QName("http://render.webservices.edu_sharing.org", "userReadAllowed"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "boolean"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
    }

    /**
     * Return type metadata object
     */
    public static org.apache.axis.description.TypeDesc getTypeDesc() {
        return typeDesc;
    }

    /**
     * Get Custom Serializer
     */
    public static org.apache.axis.encoding.Serializer getSerializer(
           java.lang.String mechType, 
           java.lang.Class _javaType,  
           javax.xml.namespace.QName _xmlType) {
        return 
          new  org.apache.axis.encoding.ser.BeanSerializer(
            _javaType, _xmlType, typeDesc);
    }

    /**
     * Get Custom Deserializer
     */
    public static org.apache.axis.encoding.Deserializer getDeserializer(
           java.lang.String mechType, 
           java.lang.Class _javaType,  
           javax.xml.namespace.QName _xmlType) {
        return 
          new  org.apache.axis.encoding.ser.BeanDeserializer(
            _javaType, _xmlType, typeDesc);
    }

}
