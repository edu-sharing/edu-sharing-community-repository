package org.edu_sharing.repository.server.importer;

public class RecordHandlerLOMWithSubObjects extends RecordHandlerLOM{

	public RecordHandlerLOMWithSubObjects(String metadataSetId) {
		super(metadataSetId);
	}

	public RecordHandlerLOMWithSubObjects(String metadataSetId, String metadataPrefix) {
		super(metadataSetId, metadataPrefix);
	}

	@Override
	public boolean createSubobjects() {
		return true;
	}
}