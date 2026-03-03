package org.edu_sharing.alfresco.policy;

import org.alfresco.service.cmr.repository.NodeRef;
import org.edu_sharing.repository.server.tools.ActionObserver;

public class ThumbnailHandling {
	
	public void thumbnailHandling(NodeRef nodeRef) {
	
		ActionObserver.getInstance().addAction(nodeRef);
	}
}
