package org.edu_sharing.service.search;

import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.AuthorityType;
import org.edu_sharing.metadataset.v2.MetadataSetV2;
import org.edu_sharing.repository.client.rpc.EduGroup;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.restservices.MdsDao;
import org.edu_sharing.restservices.shared.MdsQueryCriteria;
import org.edu_sharing.service.InsufficientPermissionException;
import org.edu_sharing.service.search.model.SearchResult;
import org.edu_sharing.service.search.model.SearchToken;
import org.edu_sharing.service.search.model.SortDefinition;

public class SearchServiceAdapter implements SearchService {

	@Override
	public SearchResult<String> searchAuthorities(AuthorityType type, String pattern, boolean globalSearch,
			int skipCount, int maxValues, SortDefinition sort) {
		return null;
	}

	@Override
	public SearchResultNodeRef search(MdsDao mdsDao, String query, List<MdsQueryCriteria> criterias, SearchToken token)
			throws Throwable {
		return null;
	}

	@Override
	public SearchResult<EduGroup> searchOrganizations(String pattern, int skipCount, int maxValues, SortDefinition sort, boolean scoped) throws Throwable {
		return null;
	}

	@Override
	public List<NodeRef> getFilesSharedByMe() throws Exception {
		return null;
	}

	@Override
	public List<NodeRef> getFilesSharedToMe() throws Exception {
		return null;
	}

	@Override
	public SearchResult<String> searchUsers(String pattern, boolean globalSearch, int skipCount, int maxValues,
			SortDefinition sort) {
		return null;
	}

	@Override
	public SearchResult<String> searchGroupMembers(String groupName, String pattern, String authorityType,
			int skipCount, int maxValues, SortDefinition sort) {
		return null;
	}

	@Override
	public SearchResultNodeRef search(SearchToken searchToken, boolean scoped) {
		return null;
	}

	@Override
	public SearchResultNodeRef search(SearchToken searchToken) {
		return null;
	}

	@Override
	public SearchResult<EduGroup> getAllOrganizations(boolean scoped) throws Exception {
		return null;
	}

	@Override
	public List<NodeRef> getWorkflowReceive(String fullyAuthenticatedUser) {
		return null;
	}

	@Override
	public SearchResultNodeRef searchV2(MetadataSetV2 mds, String query, Map<String, String[]> criterias,
			SearchToken searchToken) throws Throwable {
		return null;
	}

	@Override
	public SearchResult<String> findAuthorities(String searchWord, boolean globalContext, int from, int nrOfResults)
			throws InsufficientPermissionException {
		return null;
	}
	
}
