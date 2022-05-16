package org.edu_sharing.service.collection;

import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.model.NodeRef;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CollectionProposalInfo {
    public static class CollectionProposalData {
        private NodeRef nodeRef;
        private final Map<CCConstants.PROPOSAL_STATUS, Integer> proposalCount = new HashMap<>();

        public NodeRef getNodeRef() {
            return nodeRef;
        }

        public void setNodeRef(NodeRef nodeRef) {
            this.nodeRef = nodeRef;
        }

        public Map<CCConstants.PROPOSAL_STATUS, Integer> getProposalCount() {
            return proposalCount;
        }
    }
    private final List<CollectionProposalData> data;
    private final long totalHits;

    public CollectionProposalInfo(List<CollectionProposalData> data, long totalHits) {
        this.data = data;
        this.totalHits = totalHits;
    }

    public List<CollectionProposalData> getData() {
        return data;
    }

    public long getTotalHits() {
        return totalHits;
    }
}
