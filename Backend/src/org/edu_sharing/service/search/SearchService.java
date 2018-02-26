package org.edu_sharing.service.search;

import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.AuthorityType;
import org.edu_sharing.metadataset.v2.MetadataSetV2;
import org.edu_sharing.repository.client.rpc.EduGroup;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.restservices.DAOException;
import org.edu_sharing.restservices.MdsDao;
import org.edu_sharing.restservices.RepositoryDao;
import org.edu_sharing.restservices.iam.v1.model.UserEntries;
import org.edu_sharing.restservices.shared.MdsQueryCriteria;
import org.edu_sharing.service.InsufficientPermissionException;
import org.edu_sharing.service.search.model.SearchResult;
import org.edu_sharing.service.search.model.SearchToken;
import org.edu_sharing.service.search.model.SortDefinition;

public interface SearchService {
	public static enum ContentType{
		FILES,
		FOLDERS,
		FILES_AND_FOLDERS,
		COLLECTIONS,
		ALL
	};
	public static enum CombineMode{
		AND,
		OR
	};

	public SearchResultNodeRef search(MdsDao mdsDao, String query, List<MdsQueryCriteria> criterias, SearchToken token) throws Throwable;

	SearchResult<EduGroup> searchOrganizations(String pattern, int skipCount, int maxValues, SortDefinition sort,boolean scoped)
			throws Throwable;

	List<NodeRef> getFilesSharedByMe() throws Exception;

	List<NodeRef> getFilesSharedToMe() throws Exception;
	
	List<NodeRef> getWorkflowReceive(String fullyAuthenticatedUser);

	SearchResult<String> searchGroupMembers(String groupName, String pattern,
			String authorityType, int skipCount, int maxValues, SortDefinition sort);

	SearchResultNodeRef search(SearchToken searchToken, boolean scoped);
	SearchResultNodeRef search(SearchToken searchToken);



	SearchResult<EduGroup> getAllOrganizations(boolean scoped) throws Exception;



	SearchResultNodeRef searchV2(MetadataSetV2 mds, String query, Map<String, String[]> criterias,
			SearchToken searchToken) throws Throwable;


 	/**
      * find Authorities and Users plain solr
      * @param searchWord
      * @param globalContext
      * @param from
      * @param nrOfResults
      * @return
 	 * @throws InsufficientPermissionException 
      */
	SearchResult<String> findAuthorities(String searchWord, boolean globalContext, int from, int nrOfResults) throws InsufficientPermissionException;



	SearchResult<String> searchAuthorities(AuthorityType type, String _pattern, boolean globalSearch, int _skipCount,
			int _maxValues, SortDefinition sort, Map<String, String> customProperties);

	SearchResult<String> searchUsers(String _pattern, boolean globalSearch, int _skipCount, int _maxValues,
			SortDefinition sort, Map<String, String> customProperties);
}
