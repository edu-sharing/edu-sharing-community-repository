package org.edu_sharing.restservices.shared;

import org.codehaus.jackson.annotate.JsonProperty;

public class VersionedNode extends Node{
    @JsonProperty
    private Version version;

    public Version getVersion() {
        return version;
    }

    public void setVersion(Version version) {
        this.version = version;
    }

    public static class Version {
        private String comment;

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }
    }
}
