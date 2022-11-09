package org.edu_sharing.service.feedback.model;

import java.io.Serializable;
import java.util.Objects;

public class FeedbackResult implements Serializable {
    private String nodeId;
    /**
     * true when existing feedback was updated, false if a new one has been created
     */
    private boolean wasUpdated;

    public FeedbackResult(String nodeId, boolean wasUpdated) {
        this.nodeId = nodeId;
        this.wasUpdated = wasUpdated;
    }

    public String getNodeId() {
        return nodeId;
    }

    public boolean isWasUpdated() {
        return wasUpdated;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FeedbackResult that = (FeedbackResult) o;
        return wasUpdated == that.wasUpdated && Objects.equals(nodeId, that.nodeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeId, wasUpdated);
    }
}
