package org.edu_sharing.service.tracking;

public class NodeTrackingDetails {
    private String nodeVersion;
    // the lms which initiated the request (if any)
    private NodeTrackingLms lms;

    public NodeTrackingDetails(String nodeVersion) {
        this.nodeVersion=nodeVersion;
    }

    public String getNodeVersion() {
        return nodeVersion;
    }

    public void setNodeVersion(String nodeVersion) {
        this.nodeVersion = nodeVersion;
    }

    public NodeTrackingLms getLms() {
        return lms;
    }

    public void setLms(NodeTrackingLms lms) {
        this.lms = lms;
    }

    public static class NodeTrackingLms {
        private String appId,courseId,resourceId;

        public String getAppId() {
            return appId;
        }

        public void setAppId(String appId) {
            this.appId = appId;
        }

        public String getCourseId() {
            return courseId;
        }

        public void setCourseId(String courseId) {
            this.courseId = courseId;
        }

        public String getResourceId() {
            return resourceId;
        }

        public void setResourceId(String resourceId) {
            this.resourceId = resourceId;
        }
    }
}
