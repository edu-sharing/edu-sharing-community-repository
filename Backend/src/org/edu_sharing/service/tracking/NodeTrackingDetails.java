package org.edu_sharing.service.tracking;

import org.edu_sharing.service.usage.Usage;

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
        // since 5.1, usage is used, just a wrapper object
        private final Usage usage;

        public NodeTrackingLms(Usage usage){
            this.usage=usage;
        }

        public Usage getUsage() {
            return usage;
        }
        // for backward compatbility wrapper functions
        public String getAppId() {
            return usage.getLmsId();
        }
        public String getCourseId() {
            return usage.getCourseId();
        }
        public String getResourceId() {
            return usage.getResourceId();
        }
    }
}
