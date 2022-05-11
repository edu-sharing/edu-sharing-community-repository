package org.edu_sharing.service.model;

import org.edu_sharing.repository.client.tools.CCConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CollectionRefImpl extends NodeRefImpl implements CollectionRef {
	NodeRef relationNode;
	RelationType relationType;

	public void setRelationNode(NodeRef relationNode) {
		this.relationNode = relationNode;
	}

	@Override
	public NodeRef getRelationNode() {
		return relationNode;
	}

	public void setRelationType(RelationType relationType) {
		this.relationType = relationType;
	}

	@Override
	public RelationType getRelationType() {
		return relationType;
	}
}
