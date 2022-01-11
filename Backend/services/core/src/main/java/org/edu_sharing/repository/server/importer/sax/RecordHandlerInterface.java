package org.edu_sharing.repository.server.importer.sax;

import java.io.InputStream;
import java.util.HashMap;

import org.edu_sharing.repository.server.importer.RecordHandlerInterfaceBase;

public interface RecordHandlerInterface extends RecordHandlerInterfaceBase {

	
	public void handleRecord(InputStream isRecord) throws Throwable;
	
}
