package org.edu_sharing.service;

import org.alfresco.service.cmr.repository.StoreRef;

public class Constants {
	
	public final static StoreRef storeRef = new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore");

	public final static StoreRef archiveStoreRef = new StoreRef("archive","SpacesStore");
}
