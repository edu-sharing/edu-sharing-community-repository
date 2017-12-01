package org.edu_sharing.restservices.collection.v1;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.log4j.Logger;
import org.apache.lucene.queryParser.QueryParser;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.restservices.ApiService;
import org.edu_sharing.restservices.CollectionDao;
import org.edu_sharing.restservices.CollectionDao.Scope;
import org.edu_sharing.restservices.DAOMissingException;
import org.edu_sharing.restservices.DAOSecurityException;
import org.edu_sharing.restservices.DAOValidationException;
import org.edu_sharing.restservices.NodeDao;
import org.edu_sharing.restservices.RepositoryDao;
import org.edu_sharing.restservices.RestConstants;
import org.edu_sharing.restservices.collection.v1.model.Collection;
import org.edu_sharing.restservices.collection.v1.model.CollectionBase;
import org.edu_sharing.restservices.collection.v1.model.CollectionEntries;
import org.edu_sharing.restservices.collection.v1.model.CollectionEntry;
import org.edu_sharing.restservices.collection.v1.model.CollectionReference;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.edu_sharing.restservices.shared.Filter;
import org.edu_sharing.service.search.SearchService;
import org.edu_sharing.service.search.SearchService.ContentType;
import org.edu_sharing.service.search.SearchServiceFactory;
import org.edu_sharing.service.search.model.SearchToken;
import org.edu_sharing.service.search.model.SortDefinition;
import org.edu_sharing.service.util.AlfrescoDaoHelper;
import org.glassfish.jersey.media.multipart.FormDataParam;

import com.google.gdata.client.GDataProtocol.Error;
import com.google.gwt.user.client.rpc.core.java.util.Collections;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Path("/collection/v1")
@Api(tags = { "COLLECTION v1" })
@ApiService(value = "COLLECTION", major = 1, minor = 0)
public class CollectionApi {

	private static Logger logger = Logger.getLogger(CollectionApi.class);

	@GET
	@Path("/collections/{repository}/{collection}")
	
	@ApiOperation(value = "Get a collection.", notes = "Get a collection.")
	
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = CollectionEntry.class),
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })

	public Response getCollection(
			@ApiParam(value = "ID of repository (or \"-home-\" for home repository)", required = true, defaultValue = "-home-") @PathParam("repository") String repository,
			@ApiParam(value = "ID of collection", required = true) @PathParam("collection") String collectionId,
			@Context HttpServletRequest req) {

			try {
		
				RepositoryDao repoDao = RepositoryDao.getRepository(repository);
				
				if (repoDao == null) {
					
					return Response.status(Response.Status.NOT_FOUND).build();
				}

				CollectionDao collectionDao = CollectionDao.getCollection(repoDao,
						collectionId);

				if (collectionDao == null) {

					return Response.status(Response.Status.NOT_FOUND).build();
				}

				CollectionEntry response = new CollectionEntry();
				
				Collection collection = collectionDao.asCollection();
				
				response.setCollection(collection);

				return Response.status(Response.Status.OK).entity(response).build();
		
			} catch (Throwable t) {
	    		return ErrorResponse.createResponse(t);
	    	}
	}
	@POST
	@Path("/collections/{repository}/pinning")
	
	@ApiOperation(value = "Set pinned collections.", notes = "Remove all currently pinned collections and set them in the order send. Requires "+CCConstants.CCM_VALUE_TOOLPERMISSION_COLLECTION_PINNING)
	
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Void.class),
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })

	public Response setPinnedCollections(
			@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID,required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
			@ApiParam(value = "List of collections that should be pinned",required=true, defaultValue="-home-" ) String[] collections
			) {
		try {
			
			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
		
			if (repoDao == null) {
				return Response.status(Response.Status.NOT_FOUND).build();
			}
			CollectionDao.setPinned(repoDao,collections);
			return Response.status(Response.Status.OK).build();
	
		} catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}
	}

	@GET
	@Path("/collections/{repository}/search")
	@ApiOperation(value = "Search collections.", notes = "Search collections.")
	@ApiResponses(value = {
		@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = CollectionEntries.class),
        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	})
    public Response search(
        	@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID,required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
    	    @ApiParam(value = "query string",required=true ) @QueryParam("query") String query,
    	    @ApiParam(value = RestConstants.MESSAGE_MAX_ITEMS, defaultValue="500" ) @QueryParam("maxItems") Integer maxItems,
    	    @ApiParam(value = RestConstants.MESSAGE_SKIP_COUNT, defaultValue="0" ) @QueryParam("skipCount") Integer skipCount,
    	    @ApiParam(value = RestConstants.MESSAGE_SORT_PROPERTIES) @QueryParam("sortProperties") List<String> sortProperties,
    	    @ApiParam(value = RestConstants.MESSAGE_SORT_ASCENDING) @QueryParam("sortAscending") List<Boolean> sortAscending){
	try{
				RepositoryDao repoDao = RepositoryDao.getRepository(repository);
				
				if (repoDao == null) {
					
					return Response.status(Response.Status.NOT_FOUND).build();
				}
        		SearchService searchService = SearchServiceFactory.getSearchService(repoDao.getId());
        		SearchToken token=new SearchToken();
        		token.setContentType(ContentType.COLLECTIONS);
        		token.setFrom(skipCount!=null ? skipCount : 0);
        		token.setMaxResult(maxItems!=null ? maxItems : 500);
        		token.setSortDefinition(new SortDefinition(sortProperties, sortAscending));
				token.setLuceneString("@cm\\:name:\"*" + QueryParser.escape(query)+"*\"");
        		SearchResultNodeRef result = searchService.search(token);
        		CollectionEntries response = new CollectionEntries();
				List<Collection> collections=new ArrayList<>();
				for(NodeRef entry : AlfrescoDaoHelper.marshall(result.getData())){
					Collection collection = CollectionDao.getCollection(repoDao, entry.getId()).asCollection();
					collections.add(collection);
				}
				
				response.setCollections(collections);
				return Response.status(Response.Status.OK).entity(response).build();
        	}
	catch(Throwable t){
		return ErrorResponse.createResponse(t);
	}
}
    	    
	@PUT
	
	@Path("/collections/{repository}/{collection}")
	
	@ApiOperation(value = "Update a collection.", notes = "Update a collection.")
	
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Void.class),
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	})
	public Response updateCollection(
			@ApiParam(value = "ID of repository (or \"-home-\" for home repository)", required = true, defaultValue = "-home-") @PathParam("repository") String repository,
			@ApiParam(value = "collection", required = true) Collection collection,
			@Context HttpServletRequest req) {

			try {
		
				RepositoryDao repoDao = RepositoryDao.getRepository(repository);
				
				if (repoDao == null) {
					
					return Response.status(Response.Status.NOT_FOUND).build();
				}

				CollectionDao collectionDao = CollectionDao.getCollection(repoDao, collection.getRef().getId());

				if (collectionDao == null) {

					return Response.status(Response.Status.NOT_FOUND).build();
				}

				collectionDao.update(collection);

				return Response.status(Response.Status.OK).build();
		
			} catch (Throwable t) {
	    		return ErrorResponse.createResponse(t);
	    	}
	}

	@DELETE
	
	@Path("/collections/{repository}/{collection}")
	
	@ApiOperation(value = "Delete a collection.", notes = "Delete a collection.")
	
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Void.class),
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })

	public Response deleteCollection(
		@ApiParam(value = "ID of repository (or \"-home-\" for home repository)", required = true, defaultValue = "-home-") @PathParam("repository") String repository,
		@ApiParam(value = "ID of collection", required = true) @PathParam("collection") String collectionId,
		@Context HttpServletRequest req) {

		try {
	
			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			
			if (repoDao == null) {
				
				return Response.status(Response.Status.NOT_FOUND).build();
			}

			CollectionDao collectionDao = CollectionDao.getCollection(repoDao, collectionId);

			if (collectionDao == null) {

				return Response.status(Response.Status.NOT_FOUND).build();
			}

			collectionDao.delete();

			return Response.status(Response.Status.OK).build();
	
		} catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}
	}
	
	@OPTIONS
	
	@Path("/collections/{repository}/{collection}")
	
	@ApiOperation(hidden = true, value = "")
	
	public Response options01() {

		return Response.status(Response.Status.OK)
				.header("Allow", "OPTIONS, GET, PUT, DELETE").build();
	}

	@GET
	
	@Path("/collections/{repository}/{collection}/children")
	
	@ApiOperation(value = "Get collections.", notes = "Get collections.")
	
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = CollectionEntries.class),
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })

	public Response getCollections(
			@ApiParam(value = "ID of repository (or \"-home-\" for home repository)", required = true, defaultValue = "-home-") @PathParam("repository") String repository,
			@ApiParam(value = "ID of parent collection (or \"-root-\" for level0 collections)", required = true) @PathParam("collection") String parentId,
			@ApiParam(value = "scope", required = true) @QueryParam("scope") @DefaultValue(value = "MY") Scope scope,
			@ApiParam(value = RestConstants.MESSAGE_SORT_PROPERTIES) @QueryParam("sortProperties") List<String> sortProperties,
		    @ApiParam(value = RestConstants.MESSAGE_SORT_ASCENDING) @QueryParam("sortAscending") List<Boolean> sortAscending,
			@ApiParam(value = "property filter for result nodes (or \"-all-\" for all properties)") @QueryParam("propertyFilter") List<String> propertyFilter,
			@Context HttpServletRequest req) {

		try {

			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			
			if (repoDao == null) {
				
				return Response.status(Response.Status.NOT_FOUND).build();
			}
			SortDefinition sortDefinition = new SortDefinition(sortProperties,sortAscending);
			CollectionEntries response = new CollectionEntries();
			
			List<Collection> collections = new ArrayList<Collection>();
			List<CollectionReference> references = new ArrayList<CollectionReference>();
			
			logger.info("started DAO");
			
			Filter filter = new Filter();
			filter.setProperties(propertyFilter);
			
			for (CollectionBase item : CollectionDao.getCollections(repoDao, parentId, scope, filter,sortDefinition)) {
				
				if (item instanceof Collection) {
					collections.add((Collection) item);
				} else if (item instanceof CollectionReference) {
					references.add((CollectionReference) item);
				}
			}
			
			logger.info("finished DAO");
			
			response.setCollections(collections);
			response.setReferences(references);

			Response resp = Response.status(Response.Status.OK).entity(response).build();
			logger.info("finished building response");
			return resp;

		} catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}
	}

	@POST
	
	@Path("/collections/{repository}/{collection}/children")
	
	@ApiOperation(value = "Create a new collection.", notes = "Create a new collection.")
	
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = CollectionEntry.class),
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })

	public Response createCollection(
			@ApiParam(value = "ID of repository (or \"-home-\" for home repository)", required = true, defaultValue = "-home-") @PathParam("repository") String repository,
			@ApiParam(value = "ID of parent collection (or \"-root-\" for level0 collections)", required = true) @PathParam("collection") String parentId,
			@ApiParam(value = "collection", required = true) Collection collection,
			@Context HttpServletRequest req) {

		try {

			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			
			if (repoDao == null) {
				
				return Response.status(Response.Status.NOT_FOUND).build();
			}

			CollectionDao result = null;
			
			if (CollectionDao.ROOT.equals(parentId)) {
				
				result = CollectionDao.createRoot(repoDao, collection);
				
			} else {

				CollectionDao parentCollectionDao = CollectionDao.getCollection(repoDao, parentId);
	
				if (parentCollectionDao == null) {
	
					return Response.status(Response.Status.NOT_FOUND).build();
				}
	
				result = parentCollectionDao.createChild(collection);
			}
			
			CollectionEntry response = new CollectionEntry();
			response.setCollection(result.asCollection());

			return Response.status(Response.Status.OK).entity(response).build();

    	} catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}
	}

	@OPTIONS
	
	@Path("/collections/{repository}/{collection}/children")
	
	@ApiOperation(hidden = true, value = "")
	
	public Response options02() {

		return Response.status(Response.Status.OK)
				.header("Allow", "OPTIONS, GET, POST").build();
	}

	@PUT
	
	@Path("/collections/{repository}/{collection}/references/{node}")
	
	@ApiOperation(value = "Add a node to a collection.", notes = "Add a node to a collection.")
	
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Void.class),
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
	        @ApiResponse(code = 409, message = RestConstants.HTTP_409, response = ErrorResponse.class),        
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })

	public Response addToCollection(
			@ApiParam(value = "ID of repository (or \"-home-\" for home repository)", required = true, defaultValue = "-home-") @PathParam("repository") String repository,
			@ApiParam(value = "ID of collection", required = true) @PathParam("collection") String collectionId,
			@ApiParam(value = "ID of node", required = true) @PathParam("node") String nodeId,
			@Context HttpServletRequest req) {

		try {

			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			
			if (repoDao == null) {
				
				return Response.status(Response.Status.NOT_FOUND).build();
			}

			CollectionDao collectionDao = CollectionDao.getCollection(repoDao,
					collectionId);

			if (collectionDao == null) {

				return Response.status(Response.Status.NOT_FOUND).build();
			}

			NodeDao nodeDao = NodeDao.getNode(repoDao, nodeId);
			
			if (nodeDao == null) {

				return Response.status(Response.Status.NOT_FOUND).build();
			}
			
			collectionDao.addToCollection(nodeDao);

			return Response.status(Response.Status.OK).build();

    	} catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}
	}

	@DELETE
	
	@Path("/collections/{repository}/{collection}/references/{node}")
	
	@ApiOperation(value = "Delete a node from a collection.", notes = "Delete a node from a collection.")
	
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Void.class),
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })

	public Response deleteFromCollection(
			@ApiParam(value = "ID of repository (or \"-home-\" for home repository)", required = true, defaultValue = "-home-") @PathParam("repository") String repository,
			@ApiParam(value = "ID of collection", required = true) @PathParam("collection") String collectionId,
			@ApiParam(value = "ID of node", required = true) @PathParam("node") String nodeId,
			@Context HttpServletRequest req) {

		try {

			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			
			if (repoDao == null) {
				
				return Response.status(Response.Status.NOT_FOUND).build();
			}

			CollectionDao collectionDao = CollectionDao.getCollection(repoDao,
					collectionId);

			if (collectionDao == null) {

				return Response.status(Response.Status.NOT_FOUND).build();
			}

			NodeDao nodeDao = NodeDao.getNode(repoDao, nodeId);
			
			if (nodeDao == null) {

				return Response.status(Response.Status.NOT_FOUND).build();
			}
			
			collectionDao.removeFromCollection(nodeDao);

			return Response.status(Response.Status.OK).build();

    	} catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}
	}

	@OPTIONS
	
	@Path("/collections/{repository}/{collection}/references")
	
	@ApiOperation(hidden = true, value = "")
	
	public Response options03() {

		return Response.status(Response.Status.OK)
				.header("Allow", "OPTIONS, PUT, DELETE").build();
	}
	
	
	
	
	@POST
	@Path("/collections/{repository}/{collection}/icon")
	@Consumes({ "multipart/form-data" })
	
	@ApiOperation(value = "Writes Preview Image of a collection.", notes = "Writes Preview Image of a collection.")
	
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = CollectionEntry.class),
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })

	public Response changeIconOfCollection(
			@ApiParam(value = "ID of repository (or \"-home-\" for home repository)", required = true, defaultValue = "-home-") @PathParam("repository") String repository,
			@ApiParam(value = "ID of collection", required = true) @PathParam("collection") String collectionId,
			@ApiParam(value = "MIME-Type", required=true ) @QueryParam("mimetype")  String mimetype,
		    @FormDataParam("file") InputStream inputStream,
			@Context HttpServletRequest req) {

			try {
		
				RepositoryDao repoDao = RepositoryDao.getRepository(repository);
				
				if (repoDao == null) {
					
					return Response.status(Response.Status.NOT_FOUND).build();
				}

				CollectionDao collectionDao = CollectionDao.getCollection(repoDao,
						collectionId);

				if (collectionDao == null) {

					return Response.status(Response.Status.NOT_FOUND).build();
				}

				collectionDao.writePreviewImage(inputStream, mimetype);
				
				CollectionEntry response = new CollectionEntry();
				
				Collection collection = collectionDao.asCollection();
				
				response.setCollection(collection);

				return Response.status(Response.Status.OK).entity(response).build();
		
	    	} catch (Throwable t) {
	    		return ErrorResponse.createResponse(t);
	    	}
	}
	
	@OPTIONS    
	@Path("/collections/{repository}/{collection}/icon")
    @ApiOperation(hidden = true, value = "")
	public Response options04() {
		
		return Response.status(Response.Status.OK).header("Allow", "OPTIONS, POST").build();
	}
}
