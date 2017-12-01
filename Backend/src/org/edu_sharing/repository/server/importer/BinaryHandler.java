package org.edu_sharing.repository.server.importer;

import java.util.HashMap;

import org.w3c.dom.Node;

public interface BinaryHandler {
	public void safe(String alfrescoNodeId, HashMap<String,Object> properties,Node nodeRecord);
}
