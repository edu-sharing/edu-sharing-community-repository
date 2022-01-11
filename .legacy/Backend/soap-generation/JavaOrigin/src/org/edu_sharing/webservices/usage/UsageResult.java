/*
 * 
 */

package org.edu_sharing.webservices.usage;

public class UsageResult  implements java.io.Serializable {
    private java.lang.String appUser;

    private java.lang.String appUserMail;

    private java.lang.String courseId;

    private java.lang.Integer distinctPersons;

    private java.util.Calendar fromUsed;

    private java.lang.String lmsId;

    private java.lang.String nodeId;

    private java.util.Calendar toUsed;

    private java.lang.Integer usageCounter;

    private java.lang.String parentNodeId;

    private java.lang.String usageVersion;

    private java.lang.String usageXmlParams;

    private java.lang.String resourceId;

    public UsageResult() {
    }

    public UsageResult(
           java.lang.String appUser,
           java.lang.String appUserMail,
           java.lang.String courseId,
           java.lang.Integer distinctPersons,
           java.util.Calendar fromUsed,
           java.lang.String lmsId,
           java.lang.String nodeId,
           java.util.Calendar toUsed,
           java.lang.Integer usageCounter,
           java.lang.String parentNodeId,
           java.lang.String usageVersion,
           java.lang.String usageXmlParams,
           java.lang.String resourceId) {
           this.appUser = appUser;
           this.appUserMail = appUserMail;
           this.courseId = courseId;
           this.distinctPersons = distinctPersons;
           this.fromUsed = fromUsed;
           this.lmsId = lmsId;
           this.nodeId = nodeId;
           this.toUsed = toUsed;
           this.usageCounter = usageCounter;
           this.parentNodeId = parentNodeId;
           this.usageVersion = usageVersion;
           this.usageXmlParams = usageXmlParams;
           this.resourceId = resourceId;
    }


    /**
     * Gets the appUser value for this UsageResult.
     * 
     * @return appUser
     */
    public java.lang.String getAppUser() {
        return appUser;
    }


    /**
     * Sets the appUser value for this UsageResult.
     * 
     * @param appUser
     */
    public void setAppUser(java.lang.String appUser) {
        this.appUser = appUser;
    }


    /**
     * Gets the appUserMail value for this UsageResult.
     * 
     * @return appUserMail
     */
    public java.lang.String getAppUserMail() {
        return appUserMail;
    }


    /**
     * Sets the appUserMail value for this UsageResult.
     * 
     * @param appUserMail
     */
    public void setAppUserMail(java.lang.String appUserMail) {
        this.appUserMail = appUserMail;
    }


    /**
     * Gets the courseId value for this UsageResult.
     * 
     * @return courseId
     */
    public java.lang.String getCourseId() {
        return courseId;
    }


    /**
     * Sets the courseId value for this UsageResult.
     * 
     * @param courseId
     */
    public void setCourseId(java.lang.String courseId) {
        this.courseId = courseId;
    }


    /**
     * Gets the distinctPersons value for this UsageResult.
     * 
     * @return distinctPersons
     */
    public java.lang.Integer getDistinctPersons() {
        return distinctPersons;
    }


    /**
     * Sets the distinctPersons value for this UsageResult.
     * 
     * @param distinctPersons
     */
    public void setDistinctPersons(java.lang.Integer distinctPersons) {
        this.distinctPersons = distinctPersons;
    }


    /**
     * Gets the fromUsed value for this UsageResult.
     * 
     * @return fromUsed
     */
    public java.util.Calendar getFromUsed() {
        return fromUsed;
    }


    /**
     * Sets the fromUsed value for this UsageResult.
     * 
     * @param fromUsed
     */
    public void setFromUsed(java.util.Calendar fromUsed) {
        this.fromUsed = fromUsed;
    }


    /**
     * Gets the lmsId value for this UsageResult.
     * 
     * @return lmsId
     */
    public java.lang.String getLmsId() {
        return lmsId;
    }


    /**
     * Sets the lmsId value for this UsageResult.
     * 
     * @param lmsId
     */
    public void setLmsId(java.lang.String lmsId) {
        this.lmsId = lmsId;
    }


    /**
     * Gets the nodeId value for this UsageResult.
     * 
     * @return nodeId
     */
    public java.lang.String getNodeId() {
        return nodeId;
    }


    /**
     * Sets the nodeId value for this UsageResult.
     * 
     * @param nodeId
     */
    public void setNodeId(java.lang.String nodeId) {
        this.nodeId = nodeId;
    }


    /**
     * Gets the toUsed value for this UsageResult.
     * 
     * @return toUsed
     */
    public java.util.Calendar getToUsed() {
        return toUsed;
    }


    /**
     * Sets the toUsed value for this UsageResult.
     * 
     * @param toUsed
     */
    public void setToUsed(java.util.Calendar toUsed) {
        this.toUsed = toUsed;
    }


    /**
     * Gets the usageCounter value for this UsageResult.
     * 
     * @return usageCounter
     */
    public java.lang.Integer getUsageCounter() {
        return usageCounter;
    }


    /**
     * Sets the usageCounter value for this UsageResult.
     * 
     * @param usageCounter
     */
    public void setUsageCounter(java.lang.Integer usageCounter) {
        this.usageCounter = usageCounter;
    }


    /**
     * Gets the parentNodeId value for this UsageResult.
     * 
     * @return parentNodeId
     */
    public java.lang.String getParentNodeId() {
        return parentNodeId;
    }


    /**
     * Sets the parentNodeId value for this UsageResult.
     * 
     * @param parentNodeId
     */
    public void setParentNodeId(java.lang.String parentNodeId) {
        this.parentNodeId = parentNodeId;
    }


    /**
     * Gets the usageVersion value for this UsageResult.
     * 
     * @return usageVersion
     */
    public java.lang.String getUsageVersion() {
        return usageVersion;
    }


    /**
     * Sets the usageVersion value for this UsageResult.
     * 
     * @param usageVersion
     */
    public void setUsageVersion(java.lang.String usageVersion) {
        this.usageVersion = usageVersion;
    }


    /**
     * Gets the usageXmlParams value for this UsageResult.
     * 
     * @return usageXmlParams
     */
    public java.lang.String getUsageXmlParams() {
        return usageXmlParams;
    }


    /**
     * Sets the usageXmlParams value for this UsageResult.
     * 
     * @param usageXmlParams
     */
    public void setUsageXmlParams(java.lang.String usageXmlParams) {
        this.usageXmlParams = usageXmlParams;
    }


    /**
     * Gets the resourceId value for this UsageResult.
     * 
     * @return resourceId
     */
    public java.lang.String getResourceId() {
        return resourceId;
    }


    /**
     * Sets the resourceId value for this UsageResult.
     * 
     * @param resourceId
     */
    public void setResourceId(java.lang.String resourceId) {
        this.resourceId = resourceId;
    }
}
