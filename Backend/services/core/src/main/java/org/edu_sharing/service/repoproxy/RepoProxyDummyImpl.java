package org.edu_sharing.service.repoproxy;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import org.edu_sharing.restservices.mds.v1.model.SuggestionParam;
import org.edu_sharing.restservices.search.v1.model.SearchParameters;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.edu_sharing.service.search.SearchService.ContentType;

public class RepoProxyDummyImpl extends RepoProxyAbstract {
	
	Exception e = new Exception("not implemented");

	@Override
	public Response searchV2(String repository, String mdsId, String query, ContentType contentType, Integer maxItems,
			Integer skipCount, List<String> sortProperties, List<Boolean> sortAscending, SearchParameters parameters,
			List<String> propertyFilter, HttpServletRequest req) {
		return ErrorResponse.createResponse(e);
	}

	@Override
	public Response getDetailsSnippetWithParameters(String repository, String node, String nodeVersion,
			String displayMode, Map<String, String> parameters, HttpServletRequest req) {
		return ErrorResponse.createResponse(e);
	}

	@Override
	public Response getChildren(String repository, String node, Integer maxItems, Integer skipCount,
			List<String> filter, List<String> sortProperties, List<Boolean> sortAscending, String assocName,
			List<String> propertyFilter, HttpServletRequest req) {
		return ErrorResponse.createResponse(e);
	}

	@Override
	public Response getMetadataSetsV2(String repository, HttpServletRequest req) {
		return ErrorResponse.createResponse(e);
	}

	@Override
	public Response getMetadataSetV2(String repository, String mdsId, HttpServletRequest req) {
		return ErrorResponse.createResponse(e);
	}

	@Override
	public Response getValuesV2(String repository, String mdsId, SuggestionParam suggestionParam,
			HttpServletRequest req) {
		return ErrorResponse.createResponse(e);
	}
	
	@Override
	public Response prepareUsage(String repository, String node, HttpServletRequest req) {
		return ErrorResponse.createResponse(e);
	}

	@Override
	public Response getMetadata(String repository, String node, List<String> propertyFilter, HttpServletRequest req) {
		return ErrorResponse.createResponse(e);
	}
}
