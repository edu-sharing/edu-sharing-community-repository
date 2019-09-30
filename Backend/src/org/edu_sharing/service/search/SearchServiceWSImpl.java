package org.edu_sharing.service.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.AuthorityType;
import org.edu_sharing.metadataset.v2.MetadataSetV2;
import org.edu_sharing.repository.client.rpc.EduGroup;
import org.edu_sharing.repository.client.rpc.SearchCriterias;
import org.edu_sharing.repository.server.AuthenticationTool;
import org.edu_sharing.repository.server.RepoFactory;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.repository.server.authentication.Context;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.restservices.MdsDao;
import org.edu_sharing.restservices.shared.MdsQueryCriteria;
import org.edu_sharing.service.Constants;
import org.edu_sharing.service.InsufficientPermissionException;
import org.edu_sharing.service.search.model.SearchResult;
import org.edu_sharing.service.search.model.SearchToken;
import org.edu_sharing.service.search.model.SortDefinition;
import org.edu_sharing.service.util.AlfrescoDaoHelper;
import org.edu_sharing.webservices.alfresco.extension.Facette;
import org.edu_sharing.webservices.alfresco.extension.FacettePair;
import org.edu_sharing.webservices.alfresco.extension.NativeAlfrescoWrapper;
import org.edu_sharing.webservices.alfresco.extension.RepositoryNode;
import org.edu_sharing.webservices.alfresco.extension.SearchCriteria;
import org.edu_sharing.webservices.util.EduWebServiceFactory;

public class SearchServiceWSImpl extends SearchServiceAdapter {
	
	String appId;
	
	ApplicationInfo appInfo = null;
	
	public SearchServiceWSImpl(String appId) {
		this.appId = appId;
		appInfo = ApplicationInfoList.getRepositoryInfoById(this.appId);
	}
	
	@Override
	public SearchResult<EduGroup> getAllOrganizations(boolean scoped) throws Exception {
		return null;
	}
	
	@Override
	public SearchResultNodeRef getFilesSharedByMe(SortDefinition sortDefinition, ContentType contentType, int skipCount, int maxItems) throws Exception {
		return null;
	}
	
	@Override
	public SearchResultNodeRef getFilesSharedToMe(SortDefinition sortDefinition, ContentType contentType, int skipCount, int maxItems) throws Exception {
		return null;
	}
	
	@Override
	public SearchResultNodeRef search(MdsDao mdsDao, String query, List<MdsQueryCriteria> criterias, SearchToken token)
			throws Throwable {
		
		AuthenticationTool authTool = RepoFactory.getAuthenticationToolInstance(this.appId);
		
		//do the network auth if necessary
		HashMap<String,String> authInfo = authTool.validateAuthentication(Context.getCurrentInstance().getRequest().getSession());
		
		NativeAlfrescoWrapper stub = EduWebServiceFactory.getNativeAlfrescoWrapper(appInfo.getWebServiceHotUrl());
		
		
		List<SearchCriteria> wsCriterias = new ArrayList<SearchCriteria>();
		for(MdsQueryCriteria criteria : criterias){
			SearchCriteria sc = new SearchCriteria();
			sc.setProperty(criteria.getProperty());
			sc.setQueryId(query);
			sc.setValues(criteria.getValues().toArray(new String[0]));
			wsCriterias.add(sc);
		}
		 org.edu_sharing.webservices.alfresco.extension.SearchResult sr = stub.search(wsCriterias.toArray(new SearchCriteria[wsCriterias.size()]), 
				mdsDao.getMetadataSet().getId(), 
				token.getFrom(), token.getMaxResult(), token.getFacettes().toArray(new String[0]));
		 
		SearchResultNodeRef srnr = new SearchResultNodeRef();
		List<NodeRef> nodeRefs = new ArrayList<NodeRef>();
		for(RepositoryNode node : sr.getData()) {
			NodeRef nodeRef = new NodeRef(Constants.storeRef, node.getNodeId());
			nodeRefs.add(nodeRef);
		}
		
		Map<String,Map<String,Integer>> facettes = new HashMap<String,Map<String,Integer>>();
		for(Facette f : sr.getFacettes()){
			
			Map<String,Integer> rf = new HashMap<String,Integer>();
			for(FacettePair fp : f.getFacettePairs()){
				rf.put(fp.getValue(), fp.getCount());
			}
			
			facettes.put(f.getProperty(), rf);
		}
		
		SearchCriterias rc = new SearchCriterias();
		rc.setMetadataSetId(mdsDao.getMetadataSet().getId());
		rc.setMetadataSetQuery(query);
		rc.setRepositoryId(this.appId);

		srnr.setCountedProps(facettes);
		srnr.setData(AlfrescoDaoHelper.unmarshall(nodeRefs, this.appId));
		srnr.setNodeCount(sr.getNodeCount());
		srnr.setStartIDX(sr.getStartIDX());
		
		srnr.setSearchCriterias(rc);
			
		return srnr;
	}
	
	@Override
	public SearchResultNodeRef search(SearchToken searchToken, boolean scoped) {
		return null;
	}
	
	@Override
	public SearchResult<String> searchGroupMembers(String groupName, String pattern, String authorityType,
			int skipCount, int maxValues, SortDefinition sort) {
		return null;
	}
	
	@Override
	public SearchResult<EduGroup> searchOrganizations(String pattern, int skipCount, int maxValues, SortDefinition sort,boolean scoped, boolean onlyMemberships)
			throws Throwable {
		return null;
	}
	
	@Override
	public SearchResultNodeRef search(SearchToken _searchToken) {
		org.edu_sharing.repository.client.rpc.SearchToken searchToken = new org.edu_sharing.repository.client.rpc.SearchToken();
		searchToken.setCountProps(new ArrayList<String>(_searchToken.getFacettes()));
		searchToken.setCountPropsMinCount(_searchToken.getFacettesMinCount());
		searchToken.setNrOfResults(_searchToken.getMaxResult());
		searchToken.setRepositoryId(appId);
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
	public SearchResult<String> searchUsers(String _pattern, boolean globalSearch, int _skipCount, int _maxValues,
			SortDefinition sort, Map<String, String> customProperties) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SearchResult<String> findAuthorities(AuthorityType user, String searchWord, boolean globalContext, int from,
			int nrOfResults, SortDefinition sort, Map<String, String> customProperties)
			throws InsufficientPermissionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HashMap<ContentType,SearchToken> getLastSearchTokens() throws Throwable {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SearchResult<String> searchPersonGroups(String authorityName, String pattern, int skipCount, int maxValues,
			SortDefinition sort) {
		// TODO Auto-generated method stub
		return null;
	}

}
