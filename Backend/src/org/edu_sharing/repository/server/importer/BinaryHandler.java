package org.edu_sharing.repository.server.importer;

import org.w3c.dom.Node;

public interface BinaryHandler {
	public void safe(String alfrescoNodeId, RecordHandlerInterfaceBase recordHandler, Node nodeRecord);
}
