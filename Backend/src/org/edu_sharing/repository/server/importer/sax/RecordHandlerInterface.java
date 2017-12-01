package org.edu_sharing.repository.server.importer.sax;

import java.io.InputStream;
import java.util.HashMap;

public interface RecordHandlerInterface {

	
	public void handleRecord(InputStream isRecord) throws Throwable;
	
	public HashMap<String, Object> getProperties();
}
