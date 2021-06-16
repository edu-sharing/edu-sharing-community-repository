package org.edu_sharing.restservices.search.v1;

import java.io.Serializable;
import java.util.*;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.alfresco.rest.framework.core.exceptions.InvalidArgumentException;
import org.apache.log4j.Logger;
import org.apache.lucene.queryParser.QueryParser;
import org.edu_sharing.restservices.*;
import org.edu_sharing.restservices.node.v1.model.NodeEntry;
import org.edu_sharing.restservices.search.v1.model.SearchParameters;
import org.edu_sharing.restservices.shared.*;
import org.edu_sharing.service.repoproxy.RepoProxyFactory;
import org.edu_sharing.service.search.SearchService;
import org.edu_sharing.service.search.SearchService.CombineMode;
import org.edu_sharing.service.search.SearchServiceFactory;
import org.edu_sharing.service.search.model.SearchToken;
import org.edu_sharing.service.search.model.SearchVCard;
import org.edu_sharing.service.search.model.SortDefinition;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Path("/search/v1")
@Api(tags = { "SEARCH v1" })
@ApiService(value = "SEARCH", major = 1, minor = 0)
public class SearchApi {

	private static Logger logger = Logger.getLogger(SearchApi.class);

	@POST
	@Path("/queriesV2/{repository}/{metadataset}/{query}")
	@Consumes({ "application/json" })

	@ApiOperation(value = "Perform queries based on metadata sets V2.", notes = "Perform queries based on metadata sets V2.")

	@ApiResponses(value = { @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = SearchResultNode.class),
			@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
			@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
			@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
			@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
			@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) })

	public Response searchV2(
			@ApiParam(value = "ID of repository (or \"-home-\" for home repository)", required = true, defaultValue = "-home-") @PathParam("repository") String repository,
			@ApiParam(value = "ID of metadataset (or \"-default-\" for default metadata set)", required = true, defaultValue = "-default-") @PathParam("metadataset") String mdsId,
			@ApiParam(value = "ID of query", required = true) @PathParam("query") String query,
			@ApiParam(value = "Type of element", required = false) @QueryParam("contentType") SearchService.ContentType contentType,
			@ApiParam(value = "maximum items per page", defaultValue = "10") @QueryParam("maxItems") Integer maxItems,
			@ApiParam(value = "skip a number of items", defaultValue = "0") @QueryParam("skipCount") Integer skipCount,
			@ApiParam(value = RestConstants.MESSAGE_SORT_PROPERTIES) @QueryParam("sortProperties") List<String> sortProperties,
			@ApiParam(value = RestConstants.MESSAGE_SORT_ASCENDING) @QueryParam("sortAscending") List<Boolean> sortAscending,
			@ApiParam(value = "search parameters", required = true) SearchParameters parameters,
			@ApiParam(value = "property filter for result nodes (or \"-all-\" for all properties)", defaultValue = "-all-") @QueryParam("propertyFilter") List<String> propertyFilter,
			@Context HttpServletRequest req) {

		try {
			
			
			if(RepoProxyFactory.getRepoProxy().myTurn(repository)) {
				return RepoProxyFactory.getRepoProxy().searchV2(repository, mdsId, query, contentType, maxItems, skipCount, sortProperties, sortAscending, parameters, propertyFilter, req);
			}

			Filter filter = new Filter(propertyFilter);

			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			MdsDaoV2 mdsDao = MdsDaoV2.getMds(repoDao, mdsId);

			SearchToken token = new SearchToken();
			token.setFacettes(parameters.getFacettes());
			token.setPermissions(parameters.getPermissions());
			token.setSortDefinition(new SortDefinition(sortProperties, sortAscending));
			token.setFrom(skipCount != null ? skipCount : 0);
			token.setMaxResult(maxItems != null ? maxItems : RestConstants.DEFAULT_MAX_ITEMS);
			token.setContentType(contentType);
			NodeSearch search = NodeDao.searchV2(repoDao, mdsDao, query, parameters.getCriterias(), token, filter);

		    	List<Node> data = null;//new ArrayList<Node>();
		    	if(search.getNodes().size() < search.getResult().size()){
		    		//searched repo deliveres only nodeRefs by query time
					data = NodeDao.convertToRest(repoDao, search.getResult(), filter, null);
		    	}else{
		    		//searched repo delivered properties by query time
		    		data = search.getNodes();
		    		// @TODO: we may need to still call convertToRest to make sure we've latest data from remote repos
		    	}
		    	
		    	
		    	Pagination pagination = new Pagination();
		    	pagination.setFrom(search.getSkip());
		    	pagination.setCount(data.size());
		    	pagination.setTotal(search.getCount());
		    	
		    	
		    	SearchResultNode response = new SearchResultNode();
		    	response.setNodes(data);
		    	response.setIgnored(search.getIgnored());
		    	response.setPagination(pagination);	    	
		    	response.setFacettes(search.getFacettes());
		    	
		    	return Response.status(Response.Status.OK).entity(response).build();
		
	    	}  catch (Throwable t) {
	    		return ErrorResponse.createResponse(t);
	    	}

	}

	@POST
	@Path("/queriesV2/{repository}/{metadataset}/{query}/save")
	@Consumes({ "application/json" })

	@ApiOperation(value = "Save a search query.", notes = "Save a search query.")

	@ApiResponses(value = { @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = NodeEntry.class),
			@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
			@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
			@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
			@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
			@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) })

	public Response saveSearch(
			@ApiParam(value = "ID of repository (or \"-home-\" for home repository)", required = true, defaultValue = "-home-") @PathParam("repository") String repository,
			@ApiParam(value = "ID of metadataset (or \"-default-\" for default metadata set)", required = true, defaultValue = "-default-") @PathParam("metadataset") String mdsId,
			@ApiParam(value = "ID of query", required = true) @PathParam("query") String query,
			@ApiParam(value = "Name of the new search item", required = true) @QueryParam("name") String name,
			@ApiParam(value = "Replace if search with the same name exists", required = false, defaultValue = "false") @QueryParam("replace") Boolean replace,
			@ApiParam(value = "search parameters", required = true) List<MdsQueryCriteria> parameters,
			@Context HttpServletRequest req) {

		try {

			// RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			// MdsDao mdsDao = MdsDao.getMds(repoDao, mdsId);

			NodeDao nodeDao = NodeDao.saveSearch(repository, mdsId, query, name, parameters,
					replace != null ? replace : false);
			NodeEntry result = new NodeEntry();
			result.setNode(nodeDao.asNode());
			return Response.status(Response.Status.OK).entity(result).build();

		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}

	}

	@GET
	@Path("/queriesV2/load/{nodeId}")
	@Consumes({ "application/json" })

	@ApiOperation(value = "Load a saved search query.", notes = "Load a saved search query.")

	@ApiResponses(value = { @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Node.class),
			@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
			@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
			@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
			@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
			@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) })

	public Response loadSaveSearch(
			@ApiParam(value = "Node id of the search item", required = true) @PathParam("nodeId") String nodeId,
			@ApiParam(value = "Type of element", required = false) @QueryParam("contentType") SearchService.ContentType contentType,
			@ApiParam(value = "maximum items per page", defaultValue = "10") @QueryParam("maxItems") Integer maxItems,
			@ApiParam(value = "skip a number of items", defaultValue = "0") @QueryParam("skipCount") Integer skipCount,
			@ApiParam(value = RestConstants.MESSAGE_SORT_PROPERTIES) @QueryParam("sortProperties") List<String> sortProperties,
			@ApiParam(value = RestConstants.MESSAGE_SORT_ASCENDING) @QueryParam("sortAscending") List<Boolean> sortAscending,
			@ApiParam(value = "facettes", required = false) List<String> facettes,
			@ApiParam(value = "property filter for result nodes (or \"-all-\" for all properties)", defaultValue = "-all-") @QueryParam("propertyFilter") List<String> propertyFilter,

			@Context HttpServletRequest req) {

		try {

			Filter filter = new Filter(propertyFilter);

			SearchResult<Node> result = NodeDao.getNode(RepositoryDao.getHomeRepository(), nodeId, filter).
					runSavedSearch(skipCount != null ? skipCount : 0,
							maxItems != null ? maxItems : RestConstants.DEFAULT_MAX_ITEMS,
							contentType,
							new SortDefinition(sortProperties,sortAscending),
							facettes
							);
			return Response.status(Response.Status.OK).entity(result).build();
	    	}  catch (Throwable t) {
	    		return ErrorResponse.createResponse(t);
	    	}

	}

	@POST
	@Path("/queries/{repository}/fingerprint/{nodeid}")
	@Consumes({ "application/json" })

	@ApiOperation(value = "Perform queries based on metadata sets.", notes = "Perform queries based on metadata sets.")

	@ApiResponses(value = { @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = SearchResultNode.class),
			@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
			@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
			@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
			@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
			@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) })

	public Response searchFingerprint(
			@ApiParam(value = "ID of repository (or \"-home-\" for home repository)", required = true, defaultValue = "-home-") @PathParam("repository") String repository,
			@ApiParam(value = "nodeid", required = true) @PathParam("nodeid") String nodeId,
			@ApiParam(value = "maximum items per page", defaultValue = "10") @QueryParam("maxItems") Integer maxItems,
			@ApiParam(value = "skip a number of items", defaultValue = "0") @QueryParam("skipCount") Integer skipCount,
			@ApiParam(value = RestConstants.MESSAGE_SORT_PROPERTIES) @QueryParam("sortProperties") List<String> sortProperties,
			@ApiParam(value = RestConstants.MESSAGE_SORT_ASCENDING) @QueryParam("sortAscending") List<Boolean> sortAscending,
			@ApiParam(value = "property filter for result nodes (or \"-all-\" for all properties)", defaultValue = "-all-") @QueryParam("propertyFilter") List<String> propertyFilter,
			@Context HttpServletRequest req) {
			
			try {
				RepositoryDao repoDao = RepositoryDao.getRepository(repository);
				
				Filter filter = new Filter(propertyFilter);
				NodeSearch nodeSearch = NodeDao.searchFingerprint(repoDao, nodeId, filter);
				SearchResultNode response = new SearchResultNode();
				
				List<Node> data = new ArrayList<Node>();
				for (org.edu_sharing.restservices.shared.NodeRef ref : nodeSearch.getResult()) {
					data.add(NodeDao.getNode(repoDao, ref.getId(), filter).asNode());
				}
				response.setNodes(data);
				
				
				Pagination pagination = new Pagination();
		    	pagination.setFrom(nodeSearch.getSkip());
		    	pagination.setCount(data.size());
		    	pagination.setTotal(nodeSearch.getCount());
		    	
		    	response.setPagination(pagination);
				
				/*response.setPagination(pagination);
				response.setFacettes(search.getFacettes());
				*/
				return Response.status(Response.Status.OK).entity(response).build();
			} catch (Throwable t) {
				return ErrorResponse.createResponse(t);
			}
			
	}
	

	@GET
	@Path("/custom/{repository}")
	@Consumes({ "application/json" })

	@ApiOperation(value = "Search for custom properties with custom values", notes = "e.g. property=cm:name, value:*Test*")

	@ApiResponses(value = { @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = SearchResultNode.class),
			@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
			@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
			@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
			@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
			@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) })

	public Response searchByProperty(
			@ApiParam(value = "ID of repository (or \"-home-\" for home repository)", required = true, defaultValue = "-home-") @PathParam("repository") String repository,
			@ApiParam(value = "Type of element", required = false) @QueryParam("contentType") SearchService.ContentType contentType,
			@ApiParam(value = "Combine mode, AND or OR, defaults to AND", required = false) @QueryParam("combineMode") SearchService.CombineMode combineMode,
			@ApiParam(value = "One (or more) properties to search for, will be combined by specified combine mode", required = false) @QueryParam("property") List<String> property,
			@ApiParam(value = "One (or more) values to search for, matching the properties defined before", required = false) @QueryParam("value") List<String> value,
			@ApiParam(value = "(Optional) comparator, only relevant for date or numerical fields, currently allowed =, <=, >=", required = false) @QueryParam("comparator") List<String> comparator,
			@ApiParam(value = "maximum items per page", defaultValue = "10") @QueryParam("maxItems") Integer maxItems,
			@ApiParam(value = "skip a number of items", defaultValue = "0") @QueryParam("skipCount") Integer skipCount,
			@ApiParam(value = RestConstants.MESSAGE_SORT_PROPERTIES) @QueryParam("sortProperties") List<String> sortProperties,
			@ApiParam(value = RestConstants.MESSAGE_SORT_ASCENDING) @QueryParam("sortAscending") List<Boolean> sortAscending,
			@ApiParam(value = "property filter for result nodes (or \"-all-\" for all properties)", defaultValue = "-all-") @QueryParam("propertyFilter") List<String> propertyFilter,
			@Context HttpServletRequest req) {

		try {
			Filter filter = new Filter(propertyFilter);

			RepositoryDao repoDao = RepositoryDao.getRepository(repository);

			SearchToken token = new SearchToken();
			token.setSortDefinition(new SortDefinition(sortProperties, sortAscending));
			token.setFrom(skipCount != null ? skipCount : 0);
			token.setMaxResult(maxItems != null ? maxItems : RestConstants.DEFAULT_MAX_ITEMS);
			token.setContentType(contentType);
			if (property.size() != value.size()) {
				throw new InvalidArgumentException("Number of properties must be equal to number of values");
			}
			if (comparator != null && comparator.size() == 0)
				comparator = null;
			if (comparator != null && property.size() != comparator.size()) {
				throw new InvalidArgumentException("Number of properties must be equal to number of comparators");
			}
			String query = "";
			for (int i = 0; i < property.size(); i++) {
				String comp = "=";
				if (comparator != null)
					comp = comparator.get(i);
				if (!query.isEmpty()) {
					if (combineMode == null || combineMode.equals(CombineMode.AND)) {
						query += " AND ";
					} else {
						query += " OR ";
					}
				}
				query += "@" + property.get(i).replace(":", "\\:") + ":";
				if (comp.equals("<=")) {
					query += "[MIN TO ";
				}
				if (comp.equals(">=")) {
					query += "[";
				}
				query += QueryParser.escape(value.get(i));
				if (comp.equals("<=")) {
					query += "]";
				}
				if (comp.equals(">=")) {
					query += " TO MAX]";
				}
				query += "";
			}
			token.setLuceneString(query);
			NodeSearch search = NodeDao.search(repoDao, token);

	    	List<Node> data = new ArrayList<Node>();
	    	for (NodeRef ref : search.getResult()) {
	    		data.add(NodeDao.getNode(repoDao, ref.getId(),filter).asNode());
	    	}
	    	
	    	Pagination pagination = new Pagination();
	    	pagination.setFrom(search.getSkip());
	    	pagination.setCount(data.size());
	    	pagination.setTotal(search.getCount());


			SearchResultNode response = new SearchResultNode();
	    	response.setNodes(data);
	    	response.setPagination(pagination);	    	
	    	response.setFacettes(search.getFacettes());
	    	return Response.status(Response.Status.OK).entity(response).build();
	
    	}  catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}

	}

	@GET
	@Path("/relevant/{repository}")
	@Consumes({ "application/json" })
	@ApiOperation(value = "Get relevant nodes for the current user")
	@ApiResponses(value = { @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = SearchResultNode.class),
			@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
			@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
			@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
			@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
			@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) })
	public Response getRelevantNodes(
			@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID,required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
			@ApiParam(value = "property filter for result nodes (or \"-all-\" for all properties)", defaultValue="-all-" ) @QueryParam("propertyFilter") List<String> propertyFilter,
			@ApiParam(value = "maximum items per page", defaultValue = "10") @QueryParam("maxItems") Integer maxItems,
			@ApiParam(value = "skip a number of items", defaultValue = "0") @QueryParam("skipCount") Integer skipCount,
			@Context HttpServletRequest req) {
		try {
			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			Filter filter = new Filter(propertyFilter);
			NodeSearch nodeSearch = NodeDao.getRelevantNodes(repoDao,skipCount!=null ? skipCount : 0,maxItems!=null ? maxItems : RestConstants.DEFAULT_MAX_ITEMS);
			SearchResultNode response = new SearchResultNode();

			List<Node> data = new ArrayList<>();
			for (org.edu_sharing.restservices.shared.NodeRef ref : nodeSearch.getResult()) {
				data.add(NodeDao.getNode(repoDao, ref.getId(), filter).asNode());
			}
			response.setNodes(data);
			Pagination pagination = new Pagination();
			pagination.setFrom(nodeSearch.getSkip());
			pagination.setCount(data.size());
			pagination.setTotal(nodeSearch.getCount());
			response.setPagination(pagination);
			return Response.status(Response.Status.OK).entity(response).build();

		}   catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}

	@OPTIONS
	@Path("/custom/{repository}")
	@ApiOperation(hidden = true, value = "")

	public Response options02() {

		return Response.status(Response.Status.OK).header("Allow", "OPTIONS, GET").build();
	}


	@GET
	@Path("/queriesV2/{repository}/contributor")
	@Consumes({ "application/json" })

	@ApiOperation(value = "Search for contributors", notes = "")

	@ApiResponses(value = { @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = SearchVCard[].class),
			@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
			@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
			@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
			@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
			@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) })

	public Response searchContributor(
			@ApiParam(value = "ID of repository (or \"-home-\" for home repository)", required = true, defaultValue = "-home-") @PathParam("repository") String repository,
			@ApiParam(value = "search word", required = true) @QueryParam("searchWord") String searchWord,
			@ApiParam(value = "contributor kind", required = true, defaultValue = "PERSON") @QueryParam("contributorKind") SearchService.ContributorKind contributorKind,
			@ApiParam(value = "define which authority fields should be searched: ['firstname', 'lastname', 'email', 'uuid', 'url']") @QueryParam("fields") List<String> fields,
			@ApiParam(value = "define which contributor props should be searched: ['ccm:lifecyclecontributer_author', 'ccm:lifecyclecontributer_publisher', ..., 'ccm:metadatacontributer_creator', 'ccm:metadatacontributer_validator']") @QueryParam("contributorProperties") List<String> contributorProperties,
			@Context HttpServletRequest req) {

		try {

			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			Set<SearchVCard> result = SearchServiceFactory.getSearchService(repoDao.getId()).searchContributors(searchWord, fields, contributorProperties, contributorKind);
			return Response.status(Response.Status.OK).entity(result).build();

		}  catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}

	}

}
