package org.edu_sharing.restservices.collection.v1.model;

import org.codehaus.jackson.annotate.JsonProperty;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.restservices.shared.Node;

public class NodeProposal extends Node {
    /**
     * if this proposal is accessible by the current user
     */
    private boolean isAccessible;
    /**
     * status of this proposal
     */
    private CCConstants.PROPOSAL_STATUS status;

    /**
     * ref of the proposal node
     */
    private Node proposal;

    public NodeProposal() {
    }

    public Node getProposal() {
        return proposal;
    }

    public void setProposal(Node proposal) {
        this.proposal = proposal;
    }

    @JsonProperty
    public boolean isAccessible() {
        return isAccessible;
    }

    public void setAccessible(boolean accessible) {
        isAccessible = accessible;
    }

    @JsonProperty
    public CCConstants.PROPOSAL_STATUS getStatus() {
        return status;
    }

    public void setStatus(CCConstants.PROPOSAL_STATUS status) {
        this.status = status;
    }
}
