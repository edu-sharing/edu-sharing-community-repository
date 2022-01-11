package org.edu_sharing.service.stream;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.edu_sharing.service.stream.model.ContentEntry;
import org.edu_sharing.service.stream.model.ScoreResult;
import org.edu_sharing.service.stream.model.StreamSearchRequest;
import org.edu_sharing.service.stream.model.StreamSearchResult;
import org.edu_sharing.service.stream.model.ContentEntry.Audience.STATUS;

public interface StreamService {

	String addEntry(ContentEntry entry) throws Exception;

	void updateEntry(ContentEntry entry) throws Exception;

	ScoreResult getScoreByAuthority(String authority, STATUS status) throws Exception;

	STATUS getStatus(String id, List<String> list) throws Exception;
	
	void updateStatus(String id, String authority, STATUS status) throws Exception;

	StreamSearchResult search(StreamSearchRequest request) throws Exception;

	Map<String, Number> getTopValues(String property) throws Exception;

	ContentEntry getEntry(String entryId) throws Exception;

	boolean canAccessNode(List<String> authorities, String nodeId) throws Exception;

	void delete(String id) throws Exception;

    void deleteEntriesByAuthority(String username);
}
