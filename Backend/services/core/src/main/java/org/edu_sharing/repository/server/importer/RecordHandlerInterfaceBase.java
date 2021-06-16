package org.edu_sharing.repository.server.importer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public interface RecordHandlerInterfaceBase {

	public HashMap<String,Object> getProperties();

	default List<String> getPropertiesToRemove(){
		return new ArrayList<>();
	};

	default String getSet(){return null;};

}
