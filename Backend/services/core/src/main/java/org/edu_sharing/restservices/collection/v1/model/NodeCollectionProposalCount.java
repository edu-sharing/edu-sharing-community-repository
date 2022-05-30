package org.edu_sharing.restservices.collection.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.restservices.shared.Node;

import java.util.Map;

public class NodeCollectionProposalCount extends Node {
    private Map<CCConstants.PROPOSAL_STATUS, Integer> proposalCounts;


    @JsonProperty
    public Map<CCConstants.PROPOSAL_STATUS, Integer> getProposalCounts() {
        return proposalCounts;
    }

    public void setProposalCount(Map<CCConstants.PROPOSAL_STATUS, Integer> proposalCounts) {
        this.proposalCounts = proposalCounts;
    }
}
