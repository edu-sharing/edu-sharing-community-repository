package org.edu_sharing.restservices.search.v1;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

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
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.restservices.ApiService;
import org.edu_sharing.restservices.DAOMissingException;
import org.edu_sharing.restservices.DAOSecurityException;
import org.edu_sharing.restservices.DAOValidationException;
import org.edu_sharing.restservices.MdsDao;
import org.edu_sharing.restservices.MdsDaoV2;
import org.edu_sharing.restservices.NodeDao;
import org.edu_sharing.restservices.RepositoryDao;
import org.edu_sharing.restservices.RestConstants;
import org.edu_sharing.restservices.node.v1.model.NodeEntry;
import org.edu_sharing.restservices.search.v1.model.SearchParameters;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.edu_sharing.restservices.shared.Filter;
import org.edu_sharing.restservices.shared.MdsQueryCriteria;
import org.edu_sharing.restservices.shared.Node;
import org.edu_sharing.restservices.shared.NodeRef;
import org.edu_sharing.restservices.shared.NodeSearch;
import org.edu_sharing.restservices.shared.Pagination;
import org.edu_sharing.restservices.shared.SearchResult;
import org.edu_sharing.service.search.SearchService;
import org.edu_sharing.service.search.SearchService.CombineMode;
import org.edu_sharing.service.search.model.SearchToken;
import org.edu_sharing.service.search.model.SortDefinition;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Path("/search/v1")
@Api(tags = {"SEARCH v1"})
@ApiService(value="SEARCH", major=1, minor=0)
public class SearchApi  {

	
	private static Logger logger = Logger.getLogger(SearchApi.class);
	
	 @POST
	    @Path("/queriesV2/{repository}/{metadataset}/{query}")
	    @Consumes({ "application/json" })
	        
	    @ApiOperation(
	    	value = "Perform queries based on metadata sets V2.", 
	    	notes = "Perform queries based on metadata sets V2.")
	    
	    @ApiResponses(
	    		value = { 
	    		        @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = SearchResult.class),        
	    		        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	    		        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	    		        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	    		        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
	    		        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
		    })

	    public Response searchV2(
	    	@ApiParam(value = "ID of repository (or \"-home-\" for home repository)",required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
	    	@ApiParam(value = "ID of metadataset (or \"-default-\" for default metadata set)",required=true, defaultValue="-default-" ) @PathParam("metadataset") String mdsId,
	    	@ApiParam(value = "ID of query",required=true ) @PathParam("query") String query,
	    	@ApiParam(value = "Type of element",required=false ) @QueryParam("contentType") SearchService.ContentType contentType,
		    @ApiParam(value = "maximum items per page", defaultValue="10") @QueryParam("maxItems") Integer maxItems,
		    @ApiParam(value = "skip a number of items", defaultValue="0") @QueryParam("skipCount") Integer skipCount,
		    @ApiParam(value = RestConstants.MESSAGE_SORT_PROPERTIES) @QueryParam("sortProperties") List<String> sortProperties,
		    @ApiParam(value = RestConstants.MESSAGE_SORT_ASCENDING) @QueryParam("sortAscending") List<Boolean> sortAscending,
	    	@ApiParam(value = "search parameters",required=true ) SearchParameters parameters,
	    	@ApiParam(value = "property filter for result nodes (or \"-all-\" for all properties)", defaultValue="-all-" ) @QueryParam("propertyFilter") List<String> propertyFilter,
			@Context HttpServletRequest req) {
	    	
	    	try {
	    		
	    		Filter filter = new Filter(propertyFilter);   		
			
		    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
		    	MdsDaoV2 mdsDao = MdsDaoV2.getMds(repoDao, mdsId);

		    	SearchToken token=new SearchToken();
		    	token.setFacettes(parameters.getFacettes());
		    	token.setSortDefinition(new SortDefinition(sortProperties,sortAscending));
		    	token.setFrom(skipCount!=null ? skipCount : 0);
		    	token.setMaxResult(maxItems!=null ? maxItems : RestConstants.DEFAULT_MAX_ITEMS);
		    	token.setContentType(contentType);
		    	NodeSearch search = 
		    			NodeDao.searchV2(
		    					repoDao, 
		    					mdsDao, 
		    					query, 
		    					parameters.getCriterias(),
		    					token,filter
		    					);

		    	List<Node> data = null;//new ArrayList<Node>();
		    	if(search.getNodes().size() < search.getResult().size()){
		    		//searched repo deliveres only nodeRefs by query time
		    		data = new ArrayList<Node>();
		    		for (NodeRef ref : search.getResult()) {
			    		data.add(NodeDao.getNode(repoDao, ref.getId(),filter).asNode());
			    	}
		    	}else{
		    		//searched repo delivered properties by query time
		    		data = search.getNodes();
		    	}
		    	
		    	
		    	Pagination pagination = new Pagination();
		    	pagination.setFrom(search.getSkip());
		    	pagination.setCount(data.size());
		    	pagination.setTotal(search.getCount());
		    	
		    	
		    	SearchResult response = new SearchResult();
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
	        
	    @ApiOperation(
	    	value = "Save a search query.", 
	    	notes = "Save a search query.")
	    
	    @ApiResponses(
	    		value = { 
	    		        @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = NodeEntry.class),        
	    		        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	    		        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	    		        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	    		        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
	    		        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
		    })

	    public Response saveSearch(
	    	@ApiParam(value = "ID of repository (or \"-home-\" for home repository)",required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
	    	@ApiParam(value = "ID of metadataset (or \"-default-\" for default metadata set)",required=true, defaultValue="-default-" ) @PathParam("metadataset") String mdsId,
	    	@ApiParam(value = "ID of query",required=true ) @PathParam("query") String query,
	    	@ApiParam(value = "Name of the new search item",required=true ) @QueryParam("name") String name,
	    	@ApiParam(value = "Replace if search with the same name exists",required=false, defaultValue="false") @QueryParam("replace") Boolean replace,
	    	@ApiParam(value = "search parameters",required=true ) List<MdsQueryCriteria> parameters,
			@Context HttpServletRequest req) {
	    	
	    	try {
	    					
		    	//RepositoryDao repoDao = RepositoryDao.getRepository(repository);
		    	//MdsDao mdsDao = MdsDao.getMds(repoDao, mdsId);

		    	NodeDao nodeDao = NodeDao.saveSearch(repository,mdsId,query,name,parameters,replace!=null ? replace : false);
		    	NodeEntry result=new NodeEntry();
		    	result.setNode(nodeDao.asNode());
		    	return Response.status(Response.Status.OK).entity(result).build();
		
	    	}  catch (Throwable t) {
	    		return ErrorResponse.createResponse(t);
	    	}

	    }
	 	@GET
	    @Path("/queriesV2/load/{nodeId}")
	    @Consumes({ "application/json" })
	        
	    @ApiOperation(
	    	value = "Load a saved search query.", 
	    	notes = "Load a saved search query.")
	    
	    @ApiResponses(
	    		value = { 
	    		        @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Node.class),        
	    		        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	    		        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	    		        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	    		        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
	    		        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
		    })

	    public Response loadSaveSearch(
	    	@ApiParam(value = "Node id of the search item",required=true ) @PathParam("nodeId") String nodeId,
	    	@ApiParam(value = "Type of element",required=false ) @QueryParam("contentType") SearchService.ContentType contentType,
		    @ApiParam(value = "maximum items per page", defaultValue="10") @QueryParam("maxItems") Integer maxItems,
		    @ApiParam(value = "skip a number of items", defaultValue="0") @QueryParam("skipCount") Integer skipCount,
		    @ApiParam(value = RestConstants.MESSAGE_SORT_PROPERTIES) @QueryParam("sortProperties") List<String> sortProperties,
		    @ApiParam(value = RestConstants.MESSAGE_SORT_ASCENDING) @QueryParam("sortAscending") List<Boolean> sortAscending,
	    	@ApiParam(value = "facettes",required=false ) List<String> facettes,
	    	@ApiParam(value = "property filter for result nodes (or \"-all-\" for all properties)", defaultValue="-all-" ) @QueryParam("propertyFilter") List<String> propertyFilter,
	
			@Context HttpServletRequest req) {
	    	
	    	try {
	    					
	    		Filter filter = new Filter(propertyFilter);   		
				
	    		NodeDao saved=NodeDao.getNode(RepositoryDao.getHomeRepository(),nodeId);
	    		HashMap<String, Object> props = saved.getNativeProperties();
	    		
		    	RepositoryDao repoDao = RepositoryDao.getRepository((String) props.get(CCConstants.CCM_PROP_SAVED_SEARCH_REPOSITORY));
		    	MdsDaoV2 mdsDao = MdsDaoV2.getMds(repoDao, (String) props.get(CCConstants.CCM_PROP_SAVED_SEARCH_MDS));

		    	SearchToken token=new SearchToken();
		    	token.setFacettes(facettes);
		    	token.setSortDefinition(new SortDefinition(sortProperties,sortAscending));
		    	token.setFrom(skipCount!=null ? skipCount : 0);
		    	token.setMaxResult(maxItems!=null ? maxItems : RestConstants.DEFAULT_MAX_ITEMS);
		    	token.setContentType(contentType);
		    	
		    	ObjectMapper mapper = new ObjectMapper();
	    		List<MdsQueryCriteria> parameters = Arrays.asList(mapper.readValue((String)props.get(CCConstants.CCM_PROP_SAVED_SEARCH_PARAMETERS), MdsQueryCriteria[].class));
		    	NodeSearch search = 
		    			NodeDao.searchV2(
		    					repoDao, 
		    					mdsDao, 
		    					(String)props.get(CCConstants.CCM_PROP_SAVED_SEARCH_QUERY), 
		    					parameters, 
		    					token,filter
		    					);

		    	List<Node> data = null;//new ArrayList<Node>();
		    	if(search.getNodes().size() < search.getResult().size()){
		    		//searched repo deliveres only nodeRefs by query time
		    		data = new ArrayList<Node>();
		    		for (NodeRef ref : search.getResult()) {
			    		data.add(NodeDao.getNode(repoDao, ref.getId(),filter).asNode());
			    	}
		    	}else{
		    		//searched repo delivered properties by query time
		    		data = search.getNodes();
		    	}
		    	
		    	
		    	Pagination pagination = new Pagination();
		    	pagination.setFrom(search.getSkip());
		    	pagination.setCount(data.size());
		    	pagination.setTotal(search.getCount());
		    	
		    	
		    	SearchResult response = new SearchResult();
		    	response.setNodes(data);
		    	response.setPagination(pagination);	    	
		    	response.setFacettes(search.getFacettes());
		    	
		    	return Response.status(Response.Status.OK).entity(response).build();
		
		
	    	}  catch (Throwable t) {
	    		return ErrorResponse.createResponse(t);
	    	}

	    }
    @POST
    @Path("/queries/{repository}/{metadataset}/{query}")
    @Consumes({ "application/json" })
        
    @ApiOperation(
    	value = "Perform queries based on metadata sets.", 
    	notes = "Perform queries based on metadata sets.")
    
    @ApiResponses(
    		value = { 
    		        @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = SearchResult.class),        
    		        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
    		        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
    		        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
    		        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
    		        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })

    public Response search(
    	@ApiParam(value = "ID of repository (or \"-home-\" for home repository)",required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
    	@ApiParam(value = "ID of metadataset (or \"-default-\" for default metadata set)",required=true, defaultValue="-default-" ) @PathParam("metadataset") String mdsId,
    	@ApiParam(value = "ID of query",required=true ) @PathParam("query") String query,
    	@ApiParam(value = "Type of element",required=false ) @QueryParam("contentType") SearchService.ContentType contentType,
	    @ApiParam(value = "maximum items per page", defaultValue="10") @QueryParam("maxItems") Integer maxItems,
	    @ApiParam(value = "skip a number of items", defaultValue="0") @QueryParam("skipCount") Integer skipCount,
	    @ApiParam(value = RestConstants.MESSAGE_SORT_PROPERTIES) @QueryParam("sortProperties") List<String> sortProperties,
	    @ApiParam(value = RestConstants.MESSAGE_SORT_ASCENDING) @QueryParam("sortAscending") List<Boolean> sortAscending,
    	@ApiParam(value = "search parameters",required=true ) SearchParameters parameters,
    	@ApiParam(value = "property filter for result nodes (or \"-all-\" for all properties)", defaultValue="-all-" ) @QueryParam("propertyFilter") List<String> propertyFilter,
		@Context HttpServletRequest req) {
    	
    	try {
    		
    		Filter filter = new Filter(propertyFilter);   		
		
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	MdsDao mdsDao = MdsDao.getMds(repoDao, mdsId);

	    	SearchToken token=new SearchToken();
	    	token.setFacettes(parameters.getFacettes());
	    	token.setSortDefinition(new SortDefinition(sortProperties,sortAscending));
	    	token.setFrom(skipCount!=null ? skipCount : 0);
	    	token.setMaxResult(maxItems!=null ? maxItems : RestConstants.DEFAULT_MAX_ITEMS);
	    	token.setContentType(contentType);
	    	NodeSearch search = 
	    			NodeDao.search(
	    					repoDao, 
	    					mdsDao, 
	    					query, 
	    					parameters.getCriterias(),
	    					token,filter
	    					);

	    	List<Node> data = null;//new ArrayList<Node>();
	    	if(search.getNodes().size() < search.getResult().size()){
	    		//searched repo deliveres only nodeRefs by query time
	    		data = new ArrayList<Node>();
	    		for (NodeRef ref : search.getResult()) {
		    		data.add(NodeDao.getNode(repoDao, ref.getId(),filter).asNode());
		    	}
	    	}else{
	    		//searched repo delivered properties by query time
	    		data = search.getNodes();
	    	}
	    	
	    	
	    	Pagination pagination = new Pagination();
	    	pagination.setFrom(search.getSkip());
	    	pagination.setCount(data.size());
	    	pagination.setTotal(search.getCount());
	    	
	    	
	    	SearchResult response = new SearchResult();
	    	response.setNodes(data);
	    	response.setPagination(pagination);	    	
	    	response.setFacettes(search.getFacettes());
	    	
	    	return Response.status(Response.Status.OK).entity(response).build();
	
    	}  catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}

    }
	@OPTIONS    
	@Path("/queries/{repository}/{metadataset}/{query}")
    @ApiOperation(hidden = true, value = "")

	public Response options01() {
		
		return Response.status(Response.Status.OK).header("Allow", "OPTIONS, POST").build();
	}
    
	
    @GET
    @Path("/custom/{repository}")
    @Consumes({ "application/json" })
        
    @ApiOperation(
    	value = "Search for custom properties with custom values", 
    	notes = "e.g. property=cm:name, value:*Test*")
    
    @ApiResponses(
    	value = { 
	        @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = SearchResult.class),        
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })

    public Response searchByProperty(
    	@ApiParam(value = "ID of repository (or \"-home-\" for home repository)",required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
    	@ApiParam(value = "Type of element",required=false ) @QueryParam("contentType") SearchService.ContentType contentType,
    	@ApiParam(value = "Combine mode, AND or OR, defaults to AND",required=false ) @QueryParam("combineMode") SearchService.CombineMode combineMode,
    	@ApiParam(value = "One (or more) properties to search for, will be combined by specified combine mode",required=false ) @QueryParam("property") List<String> property,
    	@ApiParam(value = "One (or more) values to search for, matching the properties defined before",required=false ) @QueryParam("value") List<String> value,
    	@ApiParam(value = "(Optional) comparator, only relevant for date or numerical fields, currently allowed =, <=, >=",required=false ) @QueryParam("comparator") List<String> comparator,
	    @ApiParam(value = "maximum items per page", defaultValue="10") @QueryParam("maxItems") Integer maxItems,
	    @ApiParam(value = "skip a number of items", defaultValue="0") @QueryParam("skipCount") Integer skipCount,
	    @ApiParam(value = RestConstants.MESSAGE_SORT_PROPERTIES) @QueryParam("sortProperties") List<String> sortProperties,
	    @ApiParam(value = RestConstants.MESSAGE_SORT_ASCENDING) @QueryParam("sortAscending") List<Boolean> sortAscending,
    	@ApiParam(value = "property filter for result nodes (or \"-all-\" for all properties)", defaultValue="-all-" ) @QueryParam("propertyFilter") List<String> propertyFilter,
		@Context HttpServletRequest req) {
    	
    	try {
    		Filter filter = new Filter(propertyFilter);   		
		
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);

	    	SearchToken token=new SearchToken();
	    	token.setSortDefinition(new SortDefinition(sortProperties,sortAscending));
	    	token.setFrom(skipCount!=null ? skipCount : 0);
	    	token.setMaxResult(maxItems!=null ? maxItems : RestConstants.DEFAULT_MAX_ITEMS);
	    	token.setContentType(contentType);
	    	if(property.size()!=value.size()){
	    		throw new InvalidArgumentException("Number of properties must be equal to number of values");
	    	}
	    	if(comparator!=null && comparator.size()==0)
	    		comparator=null;
	    	if(comparator!=null && property.size()!=comparator.size()){
	    		throw new InvalidArgumentException("Number of properties must be equal to number of comparators");
	    	}
	    	String query="";
	    	for(int i=0;i<property.size();i++){
	    		String comp="=";
	    		if(comparator!=null)
	    			 comp=comparator.get(i);
	    		if(!query.isEmpty()){
	    			if(combineMode==null || combineMode.equals(CombineMode.AND)){
	    				query += " AND ";
	    			}
	    			else{
	    				query+=" OR ";
	    			}
	    		}
	    		query += "@"+property.get(i).replace(":", "\\:")+":";
	    		if(comp.equals("<=")){
	    			query+="[MIN TO ";
	    		}
	    		if(comp.equals(">=")){
	    			query+="[";
	    		}
	    		query+=QueryParser.escape(value.get(i));
	    		if(comp.equals("<=")){
	    			query+="]";
	    		}
	    		if(comp.equals(">=")){
	    			query+=" TO MAX]";
	    		}
	    		query+="";
	    	}
	    	token.setLuceneString(query);
	    	NodeSearch search = 
	    			NodeDao.search(
	    					repoDao, 
	    					token
	    					);

	    	List<Node> data = new ArrayList<Node>();
	    	for (NodeRef ref : search.getResult()) {
	    		data.add(NodeDao.getNode(repoDao, ref.getId(),filter).asNode());
	    	}
	    	
	    	Pagination pagination = new Pagination();
	    	pagination.setFrom(search.getSkip());
	    	pagination.setCount(data.size());
	    	pagination.setTotal(search.getCount());
	    	
	    	
	    	SearchResult response = new SearchResult();
	    	response.setNodes(data);
	    	response.setPagination(pagination);	    	
	    	response.setFacettes(search.getFacettes());
	    	return Response.status(Response.Status.OK).entity(response).build();
	
    	}  catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}

    }
    
	@OPTIONS    
    @Path("/custom/{repository}")
    @ApiOperation(hidden = true, value = "")

	public Response options02() {
		
		return Response.status(Response.Status.OK).header("Allow", "OPTIONS, GET").build();
	}
    
}

