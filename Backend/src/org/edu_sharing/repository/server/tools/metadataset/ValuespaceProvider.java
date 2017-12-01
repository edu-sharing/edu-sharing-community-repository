package org.edu_sharing.repository.server.tools.metadataset;

import java.util.List;

import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetValueKatalog;

public interface ValuespaceProvider {

	
	public List<MetadataSetValueKatalog> getValuespace(); 
	
}
