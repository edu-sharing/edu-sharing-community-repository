package org.edu_sharing.restservices.collection.v1;

import io.swagger.annotations.*;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.log4j.Logger;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.xpath.operations.Bool;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.restservices.*;
import org.edu_sharing.restservices.CollectionDao.SearchScope;
import org.edu_sharing.restservices.collection.v1.model.*;
import org.edu_sharing.restservices.node.v1.model.NodeEntry;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.edu_sharing.restservices.shared.Filter;
import org.edu_sharing.restservices.shared.Node;
import org.edu_sharing.service.search.SearchService;
import org.edu_sharing.service.search.SearchService.ContentType;
import org.edu_sharing.service.search.SearchServiceFactory;
import org.edu_sharing.service.search.model.SearchToken;
import org.edu_sharing.service.search.model.SortDefinition;
import org.edu_sharing.service.util.AlfrescoDaoHelper;
import org.glassfish.jersey.media.multipart.FormDataParam;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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

				NodeDao nodeDao = NodeDao.getNode(repoDao,
						collectionId, Filter.createShowAllFilter());

				if (nodeDao == null) {

					return Response.status(Response.Status.NOT_FOUND).build();
				}

				CollectionEntry response = new CollectionEntry();
				
				Node collection = nodeDao.asNode();
				
				response.setCollection(collection);

				return Response.status(Response.Status.OK).entity(response).build();
		
			} catch (Throwable t) {
	    		return ErrorResponse.createResponse(t);
	    	}
	}
	@POST
	@Path("/collections/{repository}/{collection}/order")
	@ApiOperation(value = "Set order of nodes in a collection. In order to work as expected, provide a list of all nodes in this collection", notes = "Current order will be overriden. Requires full permissions for the parent collection")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Void.class),
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })
	public Response setCollectionOrder(
			@ApiParam(value = "ID of repository (or \"-home-\" for home repository)", required = true, defaultValue = "-home-") @PathParam("repository") String repository,
			@ApiParam(value = "ID of collection", required = true) @PathParam("collection") String collectionId,
			@ApiParam(value = "List of nodes in the order to be saved. If empty, custom order of the collection will be disabled", required = false) String[] nodes
			) {
		try {
			
			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
		
			if (repoDao == null) {
				return Response.status(Response.Status.NOT_FOUND).build();
			}
			CollectionDao.getCollection(repoDao, collectionId).setOrder(nodes);
			return Response.status(Response.Status.OK).build();
	
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
				List<Node> collections=new ArrayList<>();
				for(NodeRef entry : AlfrescoDaoHelper.marshall(result.getData())){
					Node collection = NodeDao.getNode(repoDao, entry.getId(), Filter.createShowAllFilter()).asNode();
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
			@ApiParam(value = "collection", required = true) Node node,
			@Context HttpServletRequest req) {

			try {
		
				RepositoryDao repoDao = RepositoryDao.getRepository(repository);
				
				if (repoDao == null) {
					
					return Response.status(Response.Status.NOT_FOUND).build();
				}

				CollectionDao collectionDao = CollectionDao.getCollection(repoDao, node.getRef().getId());

				if (collectionDao == null) {

					return Response.status(Response.Status.NOT_FOUND).build();
				}

				collectionDao.update(node);

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
	@Path("/collections/{repository}/{collection}/children/references")
	@ApiOperation(value = "Get references objects for collection.")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = ReferenceEntries.class),
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })

	public Response getCollectionsReferences(
			@ApiParam(value = "ID of repository (or \"-home-\" for home repository)", required = true, defaultValue = "-home-") @PathParam("repository") String repository,
			@ApiParam(value = "ID of parent collection", required = true) @PathParam("collection") String parentId,
            @ApiParam(value = RestConstants.MESSAGE_MAX_ITEMS, defaultValue="500" ) @QueryParam("maxItems") Integer maxItems,
            @ApiParam(value = RestConstants.MESSAGE_SKIP_COUNT, defaultValue="0" ) @QueryParam("skipCount") Integer skipCount,
            @ApiParam(value = RestConstants.MESSAGE_SORT_PROPERTIES) @QueryParam("sortProperties") List<String> sortProperties,
		    @ApiParam(value = RestConstants.MESSAGE_SORT_ASCENDING) @QueryParam("sortAscending") List<Boolean> sortAscending,
			@ApiParam(value = "property filter for result nodes (or \"-all-\" for all properties)") @QueryParam("propertyFilter") List<String> propertyFilter,
			@Context HttpServletRequest req) {

		try {
			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			SortDefinition sortDefinition = new SortDefinition(sortProperties,sortAscending);
			ReferenceEntries response = new ReferenceEntries();
			List<CollectionReference> references = new ArrayList<>();
			Filter filter = new Filter();
			filter.setProperties(propertyFilter);
			CollectionBaseEntries base = CollectionDao.getCollectionsReferences(repoDao, parentId, filter, sortDefinition, skipCount == null ? 0 : skipCount, maxItems == null ? 500 : maxItems);
			for(Node item : base.getEntries()) {
					references.add((CollectionReference) item);
			}
			response.setReferences(references);
			response.setPagination(base.getPagination());
			return Response.status(Response.Status.OK).entity(response).build();

		} catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}
	}

	@GET
	@Path("/collections/{repository}/{collection}/children/collections")
	@ApiOperation(value = "Get child collections for collection (or root).")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = ReferenceEntries.class),
			@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
			@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
			@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
			@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
			@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class)
	})

	public Response getCollectionsSubcollections(
			@ApiParam(value = "ID of repository (or \"-home-\" for home repository)", required = true, defaultValue = "-home-") @PathParam("repository") String repository,
			@ApiParam(value = "ID of parent collection (or \"-root-\" for level0 collections)", required = true) @PathParam("collection") String parentId,
			@ApiParam(value = "scope (only relevant if parent == -root-)", required = true) @QueryParam("scope") @DefaultValue(value = "MY") SearchScope scope,
			@ApiParam(value = "fetch counts of collections (materials and subcollections). This parameter will decrease performance so only enable if if you need this data", required = false) @QueryParam("fetchCounts") @DefaultValue(value = "true") Boolean fetchCounts,
			@ApiParam(value = RestConstants.MESSAGE_MAX_ITEMS, defaultValue="500" ) @QueryParam("maxItems") Integer maxItems,
			@ApiParam(value = RestConstants.MESSAGE_SKIP_COUNT, defaultValue="0" ) @QueryParam("skipCount") Integer skipCount,
			@ApiParam(value = RestConstants.MESSAGE_SORT_PROPERTIES) @QueryParam("sortProperties") List<String> sortProperties,
			@ApiParam(value = RestConstants.MESSAGE_SORT_ASCENDING) @QueryParam("sortAscending") List<Boolean> sortAscending,
			@ApiParam(value = "property filter for result nodes (or \"-all-\" for all properties)") @QueryParam("propertyFilter") List<String> propertyFilter,
			@Context HttpServletRequest req) {

		try {
			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			SortDefinition sortDefinition = new SortDefinition(sortProperties,sortAscending);
			CollectionEntries response = new CollectionEntries();
			List<Node> collections = new ArrayList<>();
			Filter filter = new Filter();
			filter.setProperties(propertyFilter);
			CollectionBaseEntries base = CollectionDao.getCollectionsSubcollections(repoDao, parentId, scope,
					fetchCounts == null || fetchCounts,
					filter,
					sortDefinition,
					skipCount == null ? 0 : skipCount,
					maxItems == null ? 500 : maxItems);
			for(Node item : base.getEntries()) {
				collections.add(item);
			}
			response.setCollections(collections);
			response.setPagination(base.getPagination());
			return Response.status(Response.Status.OK).entity(response).build();

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
			@ApiParam(value = "collection", required = true) Node collection,
			@Context HttpServletRequest req) {

		try {

			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			
			if (repoDao == null) {
				
				return Response.status(Response.Status.NOT_FOUND).build();
			}

			NodeDao result = null;
			
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
			response.setCollection(result.asNode());

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
			@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = NodeEntry.class),
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
			@ApiParam(value = "ID of source repository", required=false ) @QueryParam("sourceRepo")  String sourceRepo,
			@ApiParam(value = "Allow that a node that already is inside the collection can be added again", required=false, defaultValue = "false") @QueryParam("allowDuplicate")  Boolean allowDuplicate,
			@Context HttpServletRequest req) {

		try {



			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			
			if (repoDao == null) {
				
				return Response.status(Response.Status.NOT_FOUND).build();
			}
            NodeEntry entry=new NodeEntry();

            if(sourceRepo != null && !sourceRepo.equals(RepositoryDao.getHomeRepository().getId())){
				entry.setNode(CollectionDao.addToCollection(repoDao,collectionId,nodeId,sourceRepo, allowDuplicate != null && allowDuplicate).asNode());
            }else {
                entry.setNode(CollectionDao.addToCollection(repoDao,collectionId,nodeId, null, allowDuplicate != null && allowDuplicate).asNode());
            }


            return Response.status(Response.Status.OK).entity(entry).build();

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
				
				Node collection = collectionDao.asNode();
				
				response.setCollection(collection);

				return Response.status(Response.Status.OK).entity(response).build();
		
	    	} catch (Throwable t) {
	    		return ErrorResponse.createResponse(t);
	    	}
	}
	@DELETE
	@Path("/collections/{repository}/{collection}/icon")

	@ApiOperation(value = "Deletes Preview Image of a collection.", notes = "Deletes Preview Image of a collection.")

	@ApiResponses(value = {
			@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Void.class),
			@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
			@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
			@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
			@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
			@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class)
	})

	public Response removeIconOfCollection(
			@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID, required = true, defaultValue = "-home-") @PathParam("repository") String repository,
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

			collectionDao.removePreviewImage();

			return Response.status(Response.Status.OK).build();

		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}

	@POST
	@Path("/collections/{repository}/{collection}/feedback")
	@ApiOperation(value = "Post feedback to collection.", notes = "Requires permission \"Feedback\" on the specific collection")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Void.class),
			@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
			@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
			@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
			@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
			@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class)
	})
	public Response addFeedbackToCollection(
			@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID, required = true, defaultValue = "-home-") @PathParam("repository") String repository,
			@ApiParam(value = "ID of collection", required = true) @PathParam("collection") String collectionId,
			HashMap<String,String[]> body,
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
			collectionDao.addFeedback(body);
			return Response.ok().build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}
	@GET
	@Path("/collections/{repository}/{collection}/feedback")
	@ApiOperation(value = "Get feedback of collection.", notes = "Requires permission \"???\" on the specific permission")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = CollectionFeedback[].class),
			@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
			@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
			@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
			@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
			@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class)
	})
	public Response getFeedbackOfCollection(
			@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID, required = true, defaultValue = "-home-") @PathParam("repository") String repository,
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
			return Response.ok().entity(collectionDao.getFeedbacks()).build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}
}
