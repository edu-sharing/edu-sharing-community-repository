package org.edu_sharing.restservices.collection.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.log4j.Logger;
import org.edu_sharing.metadataset.v2.MetadataSet;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.restservices.*;
import org.edu_sharing.restservices.CollectionDao.SearchScope;
import org.edu_sharing.restservices.collection.v1.model.*;
import org.edu_sharing.restservices.node.v1.model.AbstractEntries;
import org.edu_sharing.restservices.node.v1.model.NodeEntry;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.edu_sharing.restservices.shared.Filter;
import org.edu_sharing.restservices.shared.Node;
import org.edu_sharing.service.collection.CopyResult;
import org.edu_sharing.service.search.SearchService;
import org.edu_sharing.service.search.SearchService.ContentType;
import org.edu_sharing.service.search.SearchServiceFactory;
import org.edu_sharing.service.search.model.SearchToken;
import org.edu_sharing.service.search.model.SortDefinition;
import org.edu_sharing.service.tracking.ActivityEventService;
import org.edu_sharing.service.tracking.ActivityOnNodeEventType;
import org.edu_sharing.service.util.AlfrescoDaoHelper;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/collection/v1")
@Tag(name= "COLLECTION v1" )
@ApiService(value = "COLLECTION", major = 1, minor = 0)
@Consumes({ "application/json" })
@Produces({"application/json"})
public class CollectionApi {

	@Autowired
	private ActivityEventService activityEventService;

	@GET
	@Path("/collections/{repository}/{collectionId}")

	@Operation(summary = "Get a collection.", description = "Get a collection.")

	@ApiResponses(value = {
			@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = CollectionEntry.class))),
			@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	})

	public Response getCollection(
			@Parameter(description = "ID of repository (or \"-home-\" for home repository)", required = true, schema = @Schema(defaultValue="-home-")) @PathParam("repository") String repository,
			@Parameter(description = "ID of collection", required = true) @PathParam("collectionId") String collectionId,
			@Parameter(description = "track this as a view of the collection (default: true)", required = false) @QueryParam("track") Boolean track,
			@Context HttpServletRequest req) {

		try {

			RepositoryDao repoDao = RepositoryDao.getRepository(repository);

            NodeDao nodeDao = NodeDao.getNode(repoDao,
					collectionId, Filter.createShowAllFilter());

            CollectionEntry response = new CollectionEntry();

			Node collection = nodeDao.asNode();

			response.setCollection(collection);

			if(track == null || track) {
				activityEventService.trackActivityOnNode(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, collectionId), null, ActivityOnNodeEventType.VIEW_COLLECTION, AuthenticationUtil.getFullyAuthenticatedUser());
			}

			return Response.status(Response.Status.OK).entity(response).build();

		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}

	@POST
	@Path("/collections/{repository}/{collection}/order")
	@Operation(summary = "Set order of nodes in a collection. In order to work as expected, provide a list of all nodes in this collection", description = "Current order will be overriden. Requires full permissions for the parent collection")
	@ApiResponses(value = {
			@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema())),
			@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	})
	public Response setCollectionOrder(
			@Parameter(description = "ID of repository (or \"-home-\" for home repository)", required = true, schema = @Schema(defaultValue="-home-")) @PathParam("repository") String repository,
			@Parameter(description = "ID of collection", required = true) @PathParam("collection") String collectionId,
			@Parameter(description = "List of nodes in the order to be saved. If empty, custom order of the collection will be disabled", required = false) String[] nodes
	) {
		try {

			RepositoryDao repoDao = RepositoryDao.getRepository(repository);

            CollectionDao.getCollection(repoDao, collectionId).setOrder(nodes);
			return Response.status(Response.Status.OK).build();

		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}

	@POST
	@Path("/collections/{repository}/pinning")
	@Operation(summary = "Set pinned collections.", description = "Remove all currently pinned collections and set them in the order send. Requires " + CCConstants.CCM_VALUE_TOOLPERMISSION_COLLECTION_PINNING)
	@ApiResponses(value = {
			@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = Void.class))),
			@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	})
	public Response setPinnedCollections(
			@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-")) @PathParam("repository") String repository,
			@Parameter(description = "List of collections that should be pinned", required = true, array = @ArraySchema( schema = @Schema(defaultValue="-home-"))) String[] collections
	) {
		try {

			RepositoryDao repoDao = RepositoryDao.getRepository(repository);

            CollectionDao.setPinned(repoDao, collections);
			return Response.status(Response.Status.OK).build();

		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}

	/**
	 * deprecated in favour of the generic search api
	 * Use the generic search api instead
	 */
	@GET
	@Deprecated
	@Path("/collections/{repository}/search")
	@Operation(operationId = "searchCollections", summary = "Search collections.", description = "Search collections.")
	@ApiResponses(value = {
			@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = CollectionEntries.class))),
			@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	})
	public Response search(
			@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-")) @PathParam("repository") String repository,
			@Parameter(description = "query string", required = true) @QueryParam("query") String query,
			@Parameter(description = RestConstants.MESSAGE_MAX_ITEMS, schema = @Schema(defaultValue="500")) @QueryParam("maxItems") Integer maxItems,
			@Parameter(description = RestConstants.MESSAGE_SKIP_COUNT, schema = @Schema(defaultValue="0")) @QueryParam("skipCount") Integer skipCount,
			@Parameter(description = RestConstants.MESSAGE_SORT_PROPERTIES) @QueryParam("sortProperties") List<String> sortProperties,
			@Parameter(description = RestConstants.MESSAGE_SORT_ASCENDING) @QueryParam("sortAscending") List<Boolean> sortAscending) {
		try {
			RepositoryDao repoDao = RepositoryDao.getRepository(repository);

            SearchService searchService = SearchServiceFactory.getInstance().getService(repoDao.getId());


			MetadataSet mds = MdsDao.getMds(repoDao,CCConstants.metadatasetdefault_id).getMds();
			SearchToken token = new SearchToken();
			token.setContentType(ContentType.COLLECTIONS);
			token.setFrom(skipCount != null ? skipCount : 0);
			token.setMaxResult(maxItems != null ? maxItems : 500);
			token.setSortDefinition(new SortDefinition(sortProperties, sortAscending));
			Map<String, String[]> parameters = new HashMap<>();
			if(!query.trim().isEmpty())
				parameters.put("ngsearchword",new String[]{query});

			SearchResultNodeRef result = searchService.search(mds, "collections", parameters, token);

			CollectionEntries response = new CollectionEntries();
			List<Node> collections = new ArrayList<>();
			for (NodeRef entry : AlfrescoDaoHelper.marshall(result.getData())) {
				Node collection = NodeDao.getNode(repoDao, entry.getId(), Filter.createShowAllFilter()).asNode();
				collections.add(collection);
			}

			response.setCollections(collections);
			return Response.status(Response.Status.OK).entity(response).build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}

	@PUT

	@Path("/collections/{repository}/{collection}")

	@Operation(summary = "Update a collection.", description = "Update a collection.")

	@ApiResponses(value = {
			@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema())),
			@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	})
	public Response updateCollection(
			@Parameter(description = "ID of repository (or \"-home-\" for home repository)", required = true, schema = @Schema(defaultValue="-home-")) @PathParam("repository") String repository,
			@Parameter(description = "ID of collection", required = true) @PathParam("collection") String collection,
			@Parameter(description = "collection node", required = true) Node node,
			@Context HttpServletRequest req) {

		try {

			RepositoryDao repoDao = RepositoryDao.getRepository(repository);

            CollectionDao collectionDao = CollectionDao.getCollection(repoDao, node.getRef().getId());

            collectionDao.update(node);

			return Response.status(Response.Status.OK).build();

		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}

	@DELETE

	@Path("/collections/{repository}/{collection}")

	@Operation(summary = "Delete a collection.", description = "Delete a collection.")

	@ApiResponses(value = {
			@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema())),
			@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	})

	public Response deleteCollection(
			@Parameter(description = "ID of repository (or \"-home-\" for home repository)", required = true, schema = @Schema(defaultValue="-home-")) @PathParam("repository") String repository,
			@Parameter(description = "ID of collection", required = true) @PathParam("collection") String collectionId,
			@Context HttpServletRequest req) {

		try {

			RepositoryDao repoDao = RepositoryDao.getRepository(repository);

            CollectionDao collectionDao = CollectionDao.getCollection(repoDao, collectionId);

            collectionDao.delete();

			return Response.status(Response.Status.OK).build();

		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}


	@GET
	@Path("/collections/{repository}/{collection}/children/references")
	@Operation(summary = "Get references objects for collection.")
	@ApiResponses(value = {
			@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = ReferenceEntries.class))),
			@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	})

	public Response getCollectionsReferences(
			@Parameter(description = "ID of repository (or \"-home-\" for home repository)", required = true, schema = @Schema(defaultValue="-home-")) @PathParam("repository") String repository,
			@Parameter(description = "ID of parent collection", required = true) @PathParam("collection") String parentId,
			@Parameter(description = RestConstants.MESSAGE_MAX_ITEMS, schema = @Schema(defaultValue="500")) @QueryParam("maxItems") Integer maxItems,
			@Parameter(description = RestConstants.MESSAGE_SKIP_COUNT, schema = @Schema(defaultValue="0")) @QueryParam("skipCount") Integer skipCount,
			@Parameter(description = RestConstants.MESSAGE_SORT_PROPERTIES) @QueryParam("sortProperties") List<String> sortProperties,
			@Parameter(description = RestConstants.MESSAGE_SORT_ASCENDING) @QueryParam("sortAscending") List<Boolean> sortAscending,
			@Parameter(description = "property filter for result nodes (or \"-all-\" for all properties)") @QueryParam("propertyFilter") List<String> propertyFilter,
			@Context HttpServletRequest req) {

		try {
			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			SortDefinition sortDefinition = new SortDefinition(sortProperties, sortAscending);
			ReferenceEntries response = new ReferenceEntries();
			List<CollectionReference> references = new ArrayList<>();
			Filter filter = new Filter();
			filter.setProperties(propertyFilter);
			CollectionBaseEntries base = CollectionDao.getCollectionsReferences(repoDao, parentId, filter, sortDefinition, skipCount == null ? 0 : skipCount, maxItems == null ? 500 : maxItems);
			for (Node item : base.getEntries()) {
				if(item instanceof CollectionReference) {
					references.add((CollectionReference) item);
				} else {
					references.add(new CollectionReference(item));
				}
			}
			response.setReferences(references);
			response.setPagination(base.getPagination());
			return Response.status(Response.Status.OK).entity(response).build();

		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}

	@GET
	@Path("/collections/{repository}/{collection}/children/proposals")
	@Operation(summary = "Get proposed objects for collection (requires edit permissions on collection).")
	@ApiResponses(value = {
			@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = AbstractEntries.class))),
			@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	})

	public Response getCollectionsProposals(
			@Parameter(description = "ID of repository (or \"-home-\" for home repository)", required = true, schema = @Schema(defaultValue="-home-")) @PathParam("repository") String repository,
			@Parameter(description = "ID of parent collection", required = true) @PathParam("collection") String parentId,
			@Parameter(description = "Only show elements with given status", required = true) @QueryParam("status") CCConstants.PROPOSAL_STATUS status,
			@Context HttpServletRequest req) {

		try {
			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			AbstractEntries<NodeProposal> base = CollectionDao.getCollectionsProposals(repoDao, parentId, status);
			return Response.status(Response.Status.OK).entity(base).build();

		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}

	@GET
	@Path("/collections/{repository}/children/proposals/collections")
	@Operation(summary = "Get all collections containing proposals with a given state (via search index)")
	@ApiResponses(value = {
			@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = CollectionProposalEntries.class))),
			@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	})

	public Response getCollectionsContainingProposals(
			@Parameter(description = "ID of repository (or \"-home-\" for home repository)", required = true, schema = @Schema(defaultValue="-home-")) @PathParam("repository") String repository,
			@Parameter(description = "status of the proposals to search for") @QueryParam("status")  @DefaultValue(value = "PENDING") CCConstants.PROPOSAL_STATUS status,
			@Parameter(description = "fetch counts of collections (materials and subcollections). This parameter will decrease performance so only enable if if you need this data", required = false) @QueryParam("fetchCounts") @DefaultValue(value = "true") Boolean fetchCounts,
			@Parameter(description = RestConstants.MESSAGE_MAX_ITEMS, schema = @Schema(defaultValue="50")) @QueryParam("maxItems") Integer maxItems,
			@Parameter(description = RestConstants.MESSAGE_SKIP_COUNT, schema = @Schema(defaultValue="0")) @QueryParam("skipCount") Integer skipCount,
			@Parameter(description = RestConstants.MESSAGE_SORT_PROPERTIES) @QueryParam("sortProperties") List<String> sortProperties,
			@Parameter(description = RestConstants.MESSAGE_SORT_ASCENDING) @QueryParam("sortAscending") List<Boolean> sortAscending,
			@Context HttpServletRequest req) {

		try {
			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			CollectionProposalEntries entries = CollectionDao.getCollectionsContainingProposals(
					repoDao, status == null ? CCConstants.PROPOSAL_STATUS.PENDING : status,
					fetchCounts, skipCount, maxItems, new SortDefinition(sortProperties, sortAscending)
			);
			return Response.status(Response.Status.OK).entity(entries).build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}

	@GET
	@Path("/collections/{repository}/{collection}/children/collections")
	@Operation(summary = "Get child collections for collection (or root).")
	@ApiResponses(value = {
			@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = CollectionEntries.class))),
			@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	})

	public Response getCollectionsSubcollections(
			@Parameter(description = "ID of repository (or \"-home-\" for home repository)", required = true, schema = @Schema(defaultValue="-home-")) @PathParam("repository") String repository,
			@Parameter(description = "ID of parent collection (or \"-root-\" for level0 collections)", required = true) @PathParam("collection") String parentId,
			@Parameter(description = "scope (only relevant if parent == -root-)", required = true) @QueryParam("scope") @DefaultValue(value = "MY") SearchScope scope,
			@Parameter(description = "fetch counts of collections (materials and subcollections). This parameter will decrease performance so only enable if if you need this data", required = false) @QueryParam("fetchCounts") @DefaultValue(value = "true") Boolean fetchCounts,
			@Parameter(description = RestConstants.MESSAGE_MAX_ITEMS, schema = @Schema(defaultValue="500") ) @QueryParam("maxItems") Integer maxItems,
			@Parameter(description = RestConstants.MESSAGE_SKIP_COUNT, schema = @Schema(defaultValue="0"))@QueryParam("skipCount") Integer skipCount,
			@Parameter(description = RestConstants.MESSAGE_SORT_PROPERTIES) @QueryParam("sortProperties") List<String> sortProperties,
			@Parameter(description = RestConstants.MESSAGE_SORT_ASCENDING) @QueryParam("sortAscending") List<Boolean> sortAscending,
			@Parameter(description = "property filter for result nodes (or \"-all-\" for all properties)") @QueryParam("propertyFilter") List<String> propertyFilter,
			@Parameter(description = "Resolve inherited access permissions from parent nodes (default false)") @QueryParam("resolveInheritedAccess") Boolean resolveInheritedAccess,

			@Context HttpServletRequest req) {

		try {
			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			SortDefinition sortDefinition = new SortDefinition(sortProperties,sortAscending);
			CollectionEntries response = new CollectionEntries();
            Filter filter = new Filter();
			filter.setProperties(propertyFilter);
			CollectionBaseEntries base = CollectionDao.getCollectionsSubcollections(repoDao, parentId, scope,
					fetchCounts == null || fetchCounts,
					Boolean.TRUE.equals(resolveInheritedAccess),
					filter,
					sortDefinition,
					skipCount == null ? 0 : skipCount,
					maxItems == null ? 500 : maxItems);
            List<Node> collections = new ArrayList<>(base.getEntries());
			response.setCollections(collections);
			response.setPagination(base.getPagination());
			return Response.status(Response.Status.OK).entity(response).build();

		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}

	@POST
	
	@Path("/collections/{repository}/{collection}/children")
	
	@Operation(summary = "Create a new collection.", description = "Create a new collection.")
	
	@ApiResponses(value = {
			@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = CollectionEntry.class))),
	        @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
	        @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) 
	    })

	public Response createCollection(
			@Parameter(description = "ID of repository (or \"-home-\" for home repository)", required = true, schema = @Schema(defaultValue="-home-")) @PathParam("repository") String repository,
			@Parameter(description = "ID of parent collection (or \"-root-\" for level0 collections)", required = true) @PathParam("collection") String parentId,
			@Parameter(description = "collection", required = true) Node collection,
			@Context HttpServletRequest req) {

		try {

			RepositoryDao repoDao = RepositoryDao.getRepository(repository);

            NodeDao result;
			
			if (CollectionDao.ROOT.equals(parentId)) {
				
				result = CollectionDao.createRoot(repoDao, collection);
				
			} else {

				CollectionDao parentCollectionDao = CollectionDao.getCollection(repoDao, parentId);

                result = parentCollectionDao.createChild(collection);
			}
			
			CollectionEntry response = new CollectionEntry();
			response.setCollection(result.asNode());

			return Response.status(Response.Status.OK).entity(response).build();

    	} catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}
	}

	@POST

	@Path("/collections/{repository}/{sourceCollection}/copy")

	@Operation(summary = "Copy a collection.", description = "Copy a collection.")

	@ApiResponses(value = {
			@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = Copy.class))),
			@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	})

	public Response copyCollection(
			@Parameter(description = "ID of repository (or \"-home-\" for home repository)", required = true, schema = @Schema(defaultValue="-home-")) @PathParam("repository") String repository,
			@Parameter(description = "ID of parent collection (or \"-root-\" for level0 collections)", required = true) @PathParam("sourceCollection") String sourceCollection,
			@Parameter(description = "target nodeId. if not set collection will be copied in root", required = false)  @QueryParam("targetCollection") String targetCollection,
			@Parameter(description = "if true sourceCollection will be copied, else only the children", required = false, schema = @Schema(defaultValue="true")) @QueryParam("copyRoot") boolean copyRoot,
			@Parameter(description = "if true reference object will be copied",  schema = @Schema(defaultValue="false")) @QueryParam("copyRefs") boolean copyRefs,
			@Parameter(description = "if true permissions will be copied", schema = @Schema(defaultValue="false")) @QueryParam("copyPermissions") boolean copyPermissions,
            @Parameter(description = "if true child collections will be copied", schema = @Schema(defaultValue="true")) @QueryParam("copyChildCollections") boolean copyChildCollections,
			@Context HttpServletRequest req) {
		try{
			RepositoryDao repoDao = RepositoryDao.getRepository(repository);

			CopyResult copyResult = CollectionDao.copyCollection(repoDao, sourceCollection, targetCollection, copyRoot, copyRefs, copyPermissions, copyChildCollections);
			Copy copy = new Copy();
			copy.setRoot(NodeDao.getNode(repoDao,copyResult.getRoot().getId()).asNode());

			for(CopyResult.Entry e : copyResult.getEntries()){
				copy.getEntries().add(new Copy.Entry(NodeDao.getNode(repoDao,e.id()).asNode(),e.error()));
			}

			return Response.status(Response.Status.OK).entity(copy).build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}

	@PUT
	
	@Path("/collections/{repository}/{collection}/references/{node}")
	
	@Operation(summary = "Add a node to a collection.", description = "Add a node to a collection.")
	
	@ApiResponses(value = {
			@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = NodeEntry.class))),
	        @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
	        @ApiResponse(responseCode="409", description=RestConstants.HTTP_409, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) 
	    })

	public Response addToCollection(
			@Parameter(description = "ID of repository (or \"-home-\" for home repository)", required = true, schema = @Schema(defaultValue="-home-")) @PathParam("repository") String repository,
			@Parameter(description = "ID of collection", required = true) @PathParam("collection") String collectionId,
			@Parameter(description = "ID of node", required = true) @PathParam("node") String nodeId,
			@Parameter(description = "ID of source repository", required=false ) @QueryParam("sourceRepo")  String sourceRepo,
			@Parameter(description = "Allow that a node that already is inside the collection can be added again", required = false, schema = @Schema(defaultValue="false")) @QueryParam("allowDuplicate")  Boolean allowDuplicate,
			@Parameter(description = "Mark this node only as a proposal (not really adding but just marking it). This can also be used for collections where you don't have permissions", required = false, schema = @Schema(defaultValue="false")) @QueryParam("asProposal")  Boolean asProposal,
			@Context HttpServletRequest req) {

		try {



			RepositoryDao repoDao = RepositoryDao.getRepository(repository);

            NodeEntry entry=new NodeEntry();

			sourceRepo = sourceRepo != null && !sourceRepo.equals(RepositoryDao.getHomeRepository().getId()) ? sourceRepo : null;
			if(asProposal != null && asProposal) {
				CollectionDao.proposeForCollection(repoDao, collectionId, nodeId, sourceRepo);
			} else {
				entry.setNode(CollectionDao.addToCollection(repoDao, collectionId, nodeId, sourceRepo, allowDuplicate != null && allowDuplicate).asNode());
			}
            return Response.status(Response.Status.OK).entity(entry).build();

    	} catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}
	}

	@DELETE
	
	@Path("/collections/{repository}/{collection}/references/{node}")
	
	@Operation(summary = "Delete a node from a collection.", description = "Delete a node from a collection.")
	
	@ApiResponses(value = {
			@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema())),
	        @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
	        @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) 
	    })

	public Response deleteFromCollection(
			@Parameter(description = "ID of repository (or \"-home-\" for home repository)", required = true, schema = @Schema(defaultValue="-home-")) @PathParam("repository") String repository,
			@Parameter(description = "ID of collection", required = true) @PathParam("collection") String collectionId,
			@Parameter(description = "ID of node", required = true) @PathParam("node") String nodeId,
			@Context HttpServletRequest req) {

		try {

			RepositoryDao repoDao = RepositoryDao.getRepository(repository);

            CollectionDao collectionDao = CollectionDao.getCollection(repoDao,
					collectionId);

            if (collectionDao == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            if (!NodeDao.exists(repoDao, nodeId)) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

			return Response.status(Response.Status.OK).build();

    	} catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}
	}

	
	@POST
	@Path("/collections/{repository}/{collection}/icon")
	@Consumes({ "multipart/form-data" })
	
	@Operation(summary = "Writes Preview Image of a collection.", description = "Writes Preview Image of a collection.")
	
	@ApiResponses(value = {
			@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = CollectionEntry.class))),
	        @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
	        @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) 
	    })

	public Response changeIconOfCollection(
			@Parameter(description = "ID of repository (or \"-home-\" for home repository)", required = true, schema = @Schema(defaultValue="-home-")) @PathParam("repository") String repository,
			@Parameter(description = "ID of collection", required = true) @PathParam("collection") String collectionId,
			@Parameter(description = "MIME-Type", required=true ) @QueryParam("mimetype")  String mimetype,
		    @Parameter(schema = @Schema(type="string", format = "binary")) @FormDataParam("file") InputStream inputStream,
			@Context HttpServletRequest req) {

			try {
		
				RepositoryDao repoDao = RepositoryDao.getRepository(repository);

                CollectionDao collectionDao = CollectionDao.getCollection(repoDao,
						collectionId);

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

	@Operation(summary = "Deletes Preview Image of a collection.", description = "Deletes Preview Image of a collection.")

	@ApiResponses(value = {
			@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema())),
			@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	})

	public Response removeIconOfCollection(
			@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-")) @PathParam("repository") String repository,
			@Parameter(description = "ID of collection", required = true) @PathParam("collection") String collectionId,
			@Context HttpServletRequest req) {

		try {

			RepositoryDao repoDao = RepositoryDao.getRepository(repository);

            CollectionDao collectionDao = CollectionDao.getCollection(repoDao,
					collectionId);

            collectionDao.removePreviewImage();

			return Response.status(Response.Status.OK).build();

		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}
}
