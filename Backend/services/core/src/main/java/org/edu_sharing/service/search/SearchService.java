package org.edu_sharing.service.search;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.AuthorityType;
import org.edu_sharing.metadataset.v2.MetadataSetV2;
import org.edu_sharing.repository.client.rpc.EduGroup;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.restservices.shared.MdsQueryCriteria;
import org.edu_sharing.service.InsufficientPermissionException;
import org.edu_sharing.service.search.model.SearchResult;
import org.edu_sharing.service.search.model.SearchToken;
import org.edu_sharing.service.search.model.SharedToMeType;
import org.edu_sharing.service.search.model.SearchVCard;
import org.edu_sharing.service.search.model.SortDefinition;

public interface SearchService {
	public static enum ContentType{
		FILES,
		FOLDERS,
		FILES_AND_FOLDERS,
		COLLECTIONS,
		TOOLPERMISSIONS,
		ALL
	};
	public static enum CombineMode{
		AND,
		OR
	};

	public static enum ContributorKind {
		PERSON,
		ORGANIZATION
	}

	SearchResult<EduGroup> searchOrganizations(String pattern, int skipCount, int maxValues, SortDefinition sort,boolean scoped,boolean onlyMemberShips) throws Throwable;

    List<String> getAllMediacenters() throws Exception;


	SearchResultNodeRef getFilesSharedByMe(SortDefinition sortDefinition, ContentType contentType, int skipCount, int maxItems) throws Exception;

	SearchResultNodeRef getFilesSharedToMe(SharedToMeType type, SortDefinition sortDefinition, ContentType contentType, int skipCount, int maxItems) throws Exception;
	
	List<NodeRef> getWorkflowReceive(String fullyAuthenticatedUser);

	SearchResult<String> searchGroupMembers(String groupName, String pattern,
			String authorityType, int skipCount, int maxValues, SortDefinition sort);

	SearchResultNodeRef search(SearchToken searchToken, boolean scoped);
	SearchResultNodeRef search(SearchToken searchToken);



	SearchResult<EduGroup> getAllOrganizations(boolean scoped) throws Exception;

	SearchResultNodeRef getRelevantNodes(int skipCount, int maxItems) throws Throwable;

	SearchResultNodeRef searchV2(MetadataSetV2 mds, String query, Map<String, String[]> criterias,
			SearchToken searchToken) throws Throwable;


 	/**
      * find Authorities and Users plain solr
 	 * @param user
      * @param searchWord
      * @param globalContext
      * @param from
      * @param nrOfResults
 	 * @param customProperties
 	 * @param sort
      * @return
 	 * @throws InsufficientPermissionException 
      */
	SearchResult<String> findAuthorities(AuthorityType user, String searchWord, boolean globalContext, int from, int nrOfResults, SortDefinition sort, Map<String, String> customProperties) throws Exception;

	SearchResult<String> searchUsers(String _pattern, boolean globalSearch, int _skipCount, int _maxValues,
			SortDefinition sort, Map<String, String> customProperties) throws Exception;

	SearchResult<String> searchPersonGroups(String authorityName, String pattern, int skipCount, int maxValues,
			SortDefinition sort);

	HashMap<ContentType,SearchToken> getLastSearchTokens() throws Throwable;
	
	public default List<? extends  Suggestion> getSuggestions(MetadataSetV2 mds, String queryId, String parameterId, String value, List<MdsQueryCriteria> criterias) {
		return null;	
	}

	SearchResultNodeRef searchFingerPrint(String nodeId);

	public Set<SearchVCard> searchContributors(String suggest, List<String> fields, List<String> contributorProperties, ContributorKind kind) throws IOException;
}
