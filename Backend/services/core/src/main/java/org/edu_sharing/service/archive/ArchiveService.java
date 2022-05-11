package org.edu_sharing.service.archive;

import java.util.List;

import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.service.archive.model.RestoreResult;
import org.edu_sharing.service.search.model.SortDefinition;

public interface ArchiveService {
	
	public SearchResultNodeRef search(String searchWord, int from, int maxResults, SortDefinition sortDefinition);
	
	public SearchResultNodeRef search(String searchWord, String user, int from, int maxResults, SortDefinition sortDefinition);
	
	public List<RestoreResult> restore(List<String> archivedNodeIds,String toFolder);
	
	public void purge(List<String> archivedNodeIds);
}
