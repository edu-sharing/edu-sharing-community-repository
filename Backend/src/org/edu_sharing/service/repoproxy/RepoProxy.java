package org.edu_sharing.service.repoproxy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import io.swagger.client.ApiException;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.restservices.mds.v1.model.SuggestionParam;
import org.edu_sharing.restservices.search.v1.model.SearchParameters;
import org.edu_sharing.service.search.SearchService;

public interface RepoProxy {

	Response searchV2(String repository, String mdsId, String query, SearchService.ContentType contentType,
			Integer maxItems, Integer skipCount, List<String> sortProperties, List<Boolean> sortAscending,
			SearchParameters parameters, List<String> propertyFilter, HttpServletRequest req) throws Throwable;

	Response getDetailsSnippetWithParameters(String repository, String node, String nodeVersion,
			Map<String, String> parameters, HttpServletRequest req) throws Throwable;

	Response getChildren(String repository, String node, Integer maxItems, Integer skipCount, List<String> filter,
			List<String> sortProperties, List<Boolean> sortAscending, String assocName, List<String> propertyFilter,
			HttpServletRequest req) throws Throwable;

	Response getMetadataSetsV2(String repository, HttpServletRequest req) throws Throwable;

	Response getMetadataSetV2(String repository, String mdsId, HttpServletRequest req) throws Throwable;

	Response getValuesV2(String repository, String mdsId, SuggestionParam suggestionParam, HttpServletRequest req) throws Throwable;

	boolean myTurn(String repoId);
	
	public HashMap<String, String> remoteAuth(ApplicationInfo repoInfo, boolean validate) throws Throwable;

	Response prepareUsage(String repository, String node, HttpServletRequest req) throws Throwable;
	
}