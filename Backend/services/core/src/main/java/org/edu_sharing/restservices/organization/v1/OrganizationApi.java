package org.edu_sharing.restservices.organization.v1;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.rpc.EduGroup;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.restservices.ApiService;
import org.edu_sharing.restservices.OrganizationDao;
import org.edu_sharing.restservices.RepositoryDao;
import org.edu_sharing.restservices.RestConstants;
import org.edu_sharing.restservices.iam.v1.model.UserEntries;
import org.edu_sharing.restservices.organization.v1.model.OrganizationEntries;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.edu_sharing.restservices.shared.Organization;
import org.edu_sharing.restservices.shared.Pagination;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.service.search.SearchServiceFactory;
import org.edu_sharing.service.search.model.SearchResult;
import org.edu_sharing.service.search.model.SortDefinition;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Path("/organization/v1")
@Api(tags = {"ORGANIZATION v1"})
@ApiService(value="ORGANIZATION", major=1, minor=0)
public class OrganizationApi  {

	private static Logger logger = Logger.getLogger(OrganizationApi.class);

    @GET
    @Path("/organizations/{repository}")
        
    @ApiOperation(
    	value = "Get organizations of repository.", 
    	notes = "Get organizations of repository the current user is member. May returns an empty list.")
    
    @ApiResponses(
        	value = { 
    	        @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = OrganizationEntries.class),
    	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
    	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
    	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
    	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
    	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
    	    })
    
    public Response getOrganizations(
        	@ApiParam(value = "ID of repository (or \"-home-\" for home repository)",required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
        	@ApiParam(value = "pattern",required=false) @QueryParam("pattern") String pattern,
    		@ApiParam(value = RestConstants.MESSAGE_MAX_ITEMS, defaultValue=""+RestConstants.DEFAULT_MAX_ITEMS) @QueryParam("maxItems") Integer maxItems,
    	    @ApiParam(value = RestConstants.MESSAGE_SKIP_COUNT, defaultValue="0") @QueryParam("skipCount") Integer skipCount,
    	    @ApiParam(value = RestConstants.MESSAGE_SORT_PROPERTIES) @QueryParam("sortProperties") List<String> sortProperties,
    	    @ApiParam(value = RestConstants.MESSAGE_SORT_ASCENDING) @QueryParam("sortAscending") List<Boolean> sortAscending,
    	    @ApiParam(value = "search only in memberships, false can only be done by admin",defaultValue="true") @QueryParam("onlyMemberships") boolean onlyMemberships,
    		@Context HttpServletRequest req) {
    	
    	try {
    		
    		RepositoryDao repoDao = RepositoryDao.getRepository(repository);

	    	OrganizationEntries response = new OrganizationEntries();
	    	response.setCanCreate(AuthorityServiceFactory.getAuthorityService(repoDao.getId()).isGlobalAdmin());
	    	List<Organization> data = new ArrayList<Organization>();
	    	SearchResult<EduGroup> result=SearchServiceFactory.getSearchService(repoDao.getId()).searchOrganizations( 
	    			pattern, 
	    			skipCount!=null ? skipCount : 0, 
	    			maxItems!=null ? maxItems : RestConstants.DEFAULT_MAX_ITEMS, 
	    			new SortDefinition(sortProperties,sortAscending),
	    			false,
	    			onlyMemberships);
	    	for (EduGroup eduGroup : result.getData()) {
	    		data.add(new OrganizationDao(repoDao,eduGroup).asOrganization());
	    	}
	    	response.setList(data);
	    	response.setPagination(new Pagination(result));
	    	
	    	return Response.status(Response.Status.OK).entity(response).build();
	
    	} catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}

    }

	@GET
	@Path("/organizations/{repository}/{organization}")

	@ApiOperation(
			value = "Get organization by id.",
			notes = "Get organization by id.")

	@ApiResponses(
			value = {
					@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Organization.class),
					@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
					@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
					@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
					@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
					@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class)
			})

	public Response getOrganization(
			@ApiParam(value = "ID of repository (or \"-home-\" for home repository)",required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
			@ApiParam(value = "ID of organization",required=true) @PathParam("organization") String organization,
			@Context HttpServletRequest req) {

		try {

			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			return Response.status(Response.Status.OK).entity(OrganizationDao.getInstant(repoDao,organization).asOrganization()).build();

		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}

	}

    @OPTIONS    
    @Path("/organizations/{repository}")
    @ApiOperation(hidden = true, value = "")

    public Response options01() {
    		return Response.status(Response.Status.OK).header("Allow", "OPTIONS, GET").build();
    }
    
    @PUT
    @Path("/organizations/{repository}/{organization}")
        
    @ApiOperation(
    	value = "create organization in repository.", 
    	notes = "create organization in repository.")
    
    @ApiResponses(
    	value = { 
    		@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Organization.class),
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
	        @ApiResponse(code = 409, message = RestConstants.HTTP_409, response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
    	})

    public Response createOrganizations(
        	@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID,required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
    		@ApiParam(value = "organization name",required=true) @PathParam("organization") String organization,
    		@ApiParam(value = "eduscope (may be null)",defaultValue="",required=false) @QueryParam("eduscope") String scope,
    		@Context HttpServletRequest req) {
    	
    	try {


    		Organization group = (Organization)new MCAlfrescoAPIClient().doInTransaction(new RetryingTransactionCallback<Organization>() {

				@Override
				public Organization execute() throws Throwable {
					RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			    	Organization group = OrganizationDao.create(repoDao, organization,scope).asOrganization();
			    	return group;
				}
			});

	    	return Response.status(Response.Status.OK).entity(group).build();
	
    	} catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}

    }
    
    @DELETE
    @Path("/organizations/{repository}/{organization}")
        
    @ApiOperation(
    	value = "Delete organization of repository.", 
    	notes = "Delete organization of repository.")
    
    @ApiResponses(
    	value = { 
    		@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Void.class),        
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
    	})

    public Response deleteOrganizations(
        	@ApiParam(value = "ID of repository (or \"-home-\" for home repository)",required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
    		@ApiParam(value = "groupname",required=true) @PathParam("organization") String organization,
    		@Context HttpServletRequest req) {
    	
    	try {

	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	OrganizationDao organizationDao = OrganizationDao.getInstant(repoDao, organization);
	    	
	    	organizationDao.delete();
	    	
	    	return Response.status(Response.Status.OK).build();
	
    	} catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}

    }
    @DELETE
    @Path("/organizations/{repository}/{organization}/member/{member}")
        
    @ApiOperation(
    	value = "Remove member from organization.", 
    	notes = "Remove member from organization.")
    
    @ApiResponses(
    	value = { 
    		@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Void.class),        
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
    	})
    public Response removeFromOrganization(
    		@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID,required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
    		@ApiParam(value = "groupname",required=true) @PathParam("organization") String organization,
    		@ApiParam(value = "authorityName of member",required=true ) @PathParam("member") String member,
    		@Context HttpServletRequest req) {

    	try {
    		
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	OrganizationDao organizationDao = OrganizationDao.getInstant(repoDao, organization);
	    	organizationDao.removeMember(member);
	    	return Response.status(Response.Status.OK).build();
	    	
    	}  catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}
    }
    @OPTIONS    
    @Path("/organizations/{repository}/{organization}")
    @ApiOperation(hidden = true, value = "")

    public Response options02() {
    		return Response.status(Response.Status.OK).header("Allow", "OPTIONS, PUT, DELETE").build();
    }
}

