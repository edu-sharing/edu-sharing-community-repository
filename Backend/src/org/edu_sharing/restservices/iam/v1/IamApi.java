package org.edu_sharing.restservices.iam.v1;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.PermissionService;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.restservices.*;
import org.edu_sharing.restservices.iam.v1.model.AuthorityEntries;
import org.edu_sharing.restservices.iam.v1.model.GroupEntries;
import org.edu_sharing.restservices.iam.v1.model.GroupEntry;
import org.edu_sharing.restservices.iam.v1.model.Preferences;
import org.edu_sharing.restservices.iam.v1.model.ProfileSettings;
import org.edu_sharing.restservices.iam.v1.model.UserEntries;
import org.edu_sharing.restservices.iam.v1.model.UserEntry;
import org.edu_sharing.restservices.iam.v1.model.*;
import org.edu_sharing.restservices.node.v1.model.NodeEntries;
import org.edu_sharing.restservices.organization.v1.model.GroupSignupDetails;
import org.edu_sharing.restservices.shared.*;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.service.lifecycle.PersonLifecycleService;
import org.edu_sharing.service.permission.PermissionServiceFactory;
import org.edu_sharing.service.search.SearchServiceFactory;
import org.edu_sharing.service.search.model.SearchResult;
import org.edu_sharing.service.search.model.SortDefinition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Path("/iam/v1")
@Api(tags = {"IAM v1"})
@ApiService(value="IAM", major=1, minor=0)
public class IamApi  {

	private static Logger logger = Logger.getLogger(IamApi.class);

    @GET

    @Path("/people/{repository}")    
    
    @ApiOperation(
    	value = "Search users.", 
    	notes = "Search users. (admin rights are required.)")

    @ApiResponses(
    	value = { 
	        @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = UserEntries.class),        
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })

    public Response searchUser(
			@ApiParam(value = "ID of repository (or \"-home-\" for home repository)",required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
			@ApiParam(value = "pattern",required=true) @QueryParam("pattern") String pattern,
			@ApiParam(value = "global search context, defaults to true, otherwise just searches for users within the organizations",required=false,defaultValue="true") @QueryParam("global") Boolean global,
			@ApiParam(value = "the user status (e.g. active), if not set, all users are returned",required=false) @QueryParam("status") PersonLifecycleService.PersonStatus status,
			@ApiParam(value = RestConstants.MESSAGE_MAX_ITEMS, defaultValue=""+RestConstants.DEFAULT_MAX_ITEMS) @QueryParam("maxItems") Integer maxItems,
			@ApiParam(value = RestConstants.MESSAGE_SKIP_COUNT, defaultValue="0") @QueryParam("skipCount") Integer skipCount,
			@ApiParam(value = RestConstants.MESSAGE_SORT_PROPERTIES) @QueryParam("sortProperties") List<String> sortProperties,
			@ApiParam(value = RestConstants.MESSAGE_SORT_ASCENDING) @QueryParam("sortAscending") List<Boolean> sortAscending,
			@Context HttpServletRequest req) {

    	try {
    		
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			Map<String, String> filter=new HashMap<>();
			if(status!=null)
				filter.put(CCConstants.getValidLocalName(CCConstants.CM_PROP_PERSON_ESPERSONSTATUS),status.name());

			SearchResult<String> search=SearchServiceFactory.getSearchService(repoDao.getId()).searchUsers(
	    					pattern,
	    					global==null ? true : global,
	    					skipCount!=null ? skipCount : 0,
	    	    			maxItems!=null ? maxItems : RestConstants.DEFAULT_MAX_ITEMS,
	    					new SortDefinition(CCConstants.NAMESPACE_CM,sortProperties,sortAscending),
	    					filter
	    			);

	    	List<UserSimple> result = new ArrayList<UserSimple>();
	    	for (String user: search.getData()) {
	    		result.add(new PersonDao(repoDao,user).asPersonSimple());
	    	}	
	    	UserEntries response = new UserEntries();
	    	response.setList(result);
	    	response.setPagination(new Pagination(search));
	    		    	
	    	
	    	return Response.status(Response.Status.OK).entity(response).build();
	    	
   
    	} catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}
  }

    
    @OPTIONS        
    @Path("/people/{repository}")
    @ApiOperation(hidden = true, value = "")

    public Response options01() {
    	
    	return Response.status(Response.Status.OK).header("Allow", "OPTIONS, GET").build();
    }
    
    @GET
    @Path("/people/{repository}/{person}")
    @ApiOperation(
    	value = "Get the user.", 
    	notes = "Get the user. (Not all information are feteched for foreign profiles if current user is not an admin)")
    @ApiResponses(
    	value = { 
	        @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = UserEntry.class),
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class)
	    })
    public Response getUser(
    		@ApiParam(value = "ID of repository (or \"-home-\" for home repository)",required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
    		@ApiParam(value = "username (or \"-me-\" for current user)",required=true, defaultValue="-me-" ) @PathParam("person") String person,
    		@Context HttpServletRequest req) {

    	try {
    		
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	PersonDao personDao = PersonDao.getPerson(repoDao, person);
	    	
	    	UserEntry response = new UserEntry();
	    	response.setPerson(personDao.asPerson());

	    	org.edu_sharing.alfresco.repository.server.authentication.Context context =  org.edu_sharing.alfresco.repository.server.authentication.Context.getCurrentInstance();
	    String username = context.getSessionAttribute(CCConstants.AUTH_USERNAME);
	    	String authType = context.getAuthType();
		if(person.equals("-me-") || person.equals(username)) {
		 	if(authType != null && !authType.equals(CCConstants.AUTH_TYPE_DEFAULT)) {
		 		response.setEditProfile(false);
			}else {
				response.setEditProfile(true);
			}
		}


	    	return Response.status(Response.Status.OK).entity(response).build();
	    	
    	} catch (Throwable t) {
    		return ErrorResponse.createResponse(t, ErrorResponse.ErrorResponseLogging.relaxed);
    	}
    }

	@GET
	@Path("/people/{repository}/{person}/stats")
	@ApiOperation(
			value = "Get the user stats.",
			notes = "Get the user stats (e.g. publicly created material count)")
	@ApiResponses(
			value = {
					@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = UserStats.class),
					@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
					@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
					@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
					@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
					@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class)
			})
	public Response getUserStats(
			@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID,required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
			@ApiParam(value = "username (or \"-me-\" for current user)",required=true, defaultValue="-me-" ) @PathParam("person") String person,
			@Context HttpServletRequest req) {
		try {
			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			PersonDao personDao = PersonDao.getPerson(repoDao, person);
			return Response.status(Response.Status.OK).entity(personDao.getStats()).build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t, ErrorResponse.ErrorResponseLogging.relaxed);
		}
	}
    @GET       
    @Path("/people/{repository}/{person}/nodeList/{list}")    
    @ApiOperation(
    	value = "Get a specific node list for a user", 
    	notes = "For guest users, the list will be temporary stored in the current session")
    
    @ApiResponses(
    	value = { 
    			 @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = NodeEntries.class),        
    		        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
    		        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
    		        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
    		        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
    		        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
    		    })

    public Response getNodeList(
    		@ApiParam(value = "ID of repository (or \"-home-\" for home repository)",required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
    		@ApiParam(value = "username (or \"-me-\" for current user)",required=true, defaultValue="-me-" ) @PathParam("person") String person,
    		@ApiParam(value = "list name",required=true) @PathParam("list") String list,
    	    @ApiParam(value = RestConstants.MESSAGE_PROPERTY_FILTER, defaultValue="-all-" ) @QueryParam("propertyFilter") List<String> propertyFilter,
    		@ApiParam(value = RestConstants.MESSAGE_SORT_PROPERTIES) @QueryParam("sortProperties") List<String> sortProperties,
    		@ApiParam(value = RestConstants.MESSAGE_SORT_ASCENDING) @QueryParam("sortAscending") List<Boolean> sortAscending,
    		@Context HttpServletRequest req) {
		try{
			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	PersonDao personDao = PersonDao.getPerson(repoDao, person);
			SortDefinition sortDefinition = new SortDefinition(sortProperties,sortAscending);
    		Filter propFilter = new Filter(propertyFilter);
	    	NodeEntries result=new NodeEntries();
	    	List<NodeRef> refList = personDao.getNodeList(list);
	    	if(refList!=null){
                refList=NodeDao.sortApiNodeRefs(repoDao,refList,null,sortDefinition);
	    	    result=NodeDao.convertToRest(repoDao,propFilter,refList,0,Integer.MAX_VALUE);
	    	}
	    	return Response.status(Response.Status.OK).entity(result).build();
		}catch(Throwable t){
			return ErrorResponse.createResponse(t);
		}
    }
    @PUT       
    @Path("/people/{repository}/{person}/nodeList/{list}/{node}")    
    @ApiOperation(
    	value = "Add a node to node a list of a user", 
    	notes = "For guest users, the list will be temporary stored in the current session")
    
    @ApiResponses(
    	value = { 
    			 @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Void.class),        
    		        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
    		        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
    		        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
    		        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
    		        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
    		    })

    public Response addNodeList(
    		@ApiParam(value = "ID of repository (or \"-home-\" for home repository)",required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
    		@ApiParam(value = "username (or \"-me-\" for current user)",required=true, defaultValue="-me-" ) @PathParam("person") String person,
    		@ApiParam(value = "list name. If this list does not exist, it will be created",required=true) @PathParam("list") String list,    		
    		@ApiParam(value = RestConstants.MESSAGE_NODE_ID,required=true) @PathParam("node") String node,    		
    		@Context HttpServletRequest req) {
		try{
			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	PersonDao personDao = PersonDao.getPerson(repoDao, person);
	    	personDao.addNodeList(list, node);
	    	return Response.status(Response.Status.OK).build();
		}catch(Throwable t){
			return ErrorResponse.createResponse(t);
		}
    }
    @DELETE       
    @Path("/people/{repository}/{person}/nodeList/{list}/{node}")    
    @ApiOperation(
    	value = "Deelete a node of a node list of a user", 
    	notes = "For guest users, the list will be temporary stored in the current session")
    
    @ApiResponses(
    	value = { 
    			 @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Void.class),        
    		        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
    		        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
    		        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
    		        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
    		        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
    		    })

    public Response removeNodeList(
    		@ApiParam(value = "ID of repository (or \"-home-\" for home repository)",required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
    		@ApiParam(value = "username (or \"-me-\" for current user)",required=true, defaultValue="-me-" ) @PathParam("person") String person,
    		@ApiParam(value = "list name",required=true) @PathParam("list") String list,    		
    		@ApiParam(value = RestConstants.MESSAGE_NODE_ID,required=true) @PathParam("node") String node,    		
    		@Context HttpServletRequest req) {
		try{
			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	PersonDao personDao = PersonDao.getPerson(repoDao, person);
	    	personDao.removeNodeList(list, node);
	    	return Response.status(Response.Status.OK).build();
		}catch(Throwable t){
			return ErrorResponse.createResponse(t);
		}
    }
    @GET       
    @Path("/people/{repository}/{person}/preferences")    
    @ApiOperation(
    	value = "Get preferences stored for user", 
    	notes = "Will fail for guest")
    
    @ApiResponses(
    	value = { 
    			 @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Preferences.class),        
    		        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
    		        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
    		        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
    		        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
    		        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
    		    })

    public Response getPreferences(
    		@ApiParam(value = "ID of repository (or \"-home-\" for home repository)",required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
    		@ApiParam(value = "username (or \"-me-\" for current user)",required=true, defaultValue="-me-" ) @PathParam("person") String person,
    		@Context HttpServletRequest req) {
		try{
			org.edu_sharing.service.authority.AuthorityService service=AuthorityServiceFactory.getAuthorityService(ApplicationInfoList.getHomeRepository().getAppId());
			if(service.isGuest())
				throw new Exception("Not allowed for guest user");
			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	PersonDao personDao = PersonDao.getPerson(repoDao, person);
	    	String preferences=personDao.getPreferences();
	    	Preferences pref=new Preferences();
	    	pref.setPreferences(preferences);
	    	return Response.status(Response.Status.OK).entity(pref).build();
		}catch(Throwable t){
			return ErrorResponse.createResponse(t);
		}
    }
    @PUT       
    @Path("/people/{repository}/{person}/preferences")    
    @ApiOperation(
    	value = "Set preferences for user", 
    	notes = "Will fail for guest")
    
    @ApiResponses(
    	value = { 
    			 @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Void.class),        
    		        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
    		        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
    		        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
    		        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
    		        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
    		    })

    public Response setPreferences(
    		@ApiParam(value = "ID of repository (or \"-home-\" for home repository)",required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
    		@ApiParam(value = "username (or \"-me-\" for current user)",required=true, defaultValue="-me-" ) @PathParam("person") String person,
    		@ApiParam(value = "preferences (json string)",required=true, defaultValue="-me-" ) String content,
    		@Context HttpServletRequest req) {
		try{
			org.edu_sharing.service.authority.AuthorityService service=AuthorityServiceFactory.getAuthorityService(ApplicationInfoList.getHomeRepository().getAppId());
			if(service.isGuest())
				throw new Exception("Not allowed for guest user");
			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	PersonDao personDao = PersonDao.getPerson(repoDao, person);
	    	personDao.setPreferences(content);
	    	return Response.status(Response.Status.OK).build();
		}catch(Throwable t){
			return ErrorResponse.createResponse(t);
		}
    }

	@GET
	@Path("/people/{repository}/{person}/profileSettings")
	@ApiOperation(
			value = "Get profileSettings configuration",
			notes = "Will fail for guest")

	@ApiResponses(
			value = {
					@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = ProfileSettings.class),
					@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
					@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
					@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
					@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
					@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class)
			})

	public Response getProfileSettings(
			@ApiParam(value = "ID of repository (or \"-home-\" for home repository)", required = true, defaultValue = "-home-") @PathParam("repository") String repository,
			@ApiParam(value = "username (or \"-me-\" for current user)", required = true, defaultValue = "-me-") @PathParam("person") String person,
			@Context HttpServletRequest req) {
		try {
			org.edu_sharing.service.authority.AuthorityService service = AuthorityServiceFactory.getAuthorityService(ApplicationInfoList.getHomeRepository().getAppId());
			if (service.isGuest())
				throw new Exception("Not allowed for guest user");
			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			PersonDao personDao = PersonDao.getPerson(repoDao, person);
			ProfileSettings personDaoProfileSettings = personDao.getProfileSettings();
			return Response.status(Response.Status.OK).entity(personDaoProfileSettings).build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}


	@PUT
	@Path("/people/{repository}/{person}/profileSettings")
	@ApiOperation(
			value = "Set profileSettings Configuration",
			notes = "Will fail for guest")

	@ApiResponses(
			value = {
					@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Void.class),
					@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
					@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
					@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
					@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
					@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class)
			})

	public Response setProfileSettings(
			@ApiParam(value = "ID of repository (or \"-home-\" for home repository)", required = true, defaultValue = "-home-") @PathParam("repository") String repository,
			@ApiParam(value = "username (or \"-me-\" for current user)", required = true, defaultValue = "-me-") @PathParam("person") String person,
			@ApiParam(value = "ProfileSetting Object", required = true) ProfileSettings profileSettings,
			@Context HttpServletRequest req) {
		try {
			org.edu_sharing.service.authority.AuthorityService service = AuthorityServiceFactory.getAuthorityService(ApplicationInfoList.getHomeRepository().getAppId());
			if (service.isGuest())
				throw new Exception("Not allowed for guest user");
			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			PersonDao personDao = PersonDao.getPerson(repoDao, person);
			personDao.setProfileSettings(profileSettings);
			return Response.status(Response.Status.OK).build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}



    @POST

    @Path("/people/{repository}/{person}")    
    
    @ApiOperation(
    	value = "Create a new user.", 
    	notes = "Create a new user. (admin rights are required.)")

    @ApiResponses(
    	value = { 
	        @ApiResponse(code = 200, message = "OK.", response = User.class),
	        @ApiResponse(code = 400, message = "Preconditions are not present.", response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = "Authorization failed.", response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = "Session user has insufficient rights to perform this operation.", response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = "Ressources are not found.", response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = "Fatal error occured.", response = ErrorResponse.class) 
	    })

    public Response createUser(
    		@ApiParam(value = "ID of repository (or \"-home-\" for home repository)",required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
    		@ApiParam(value = "username",required=true) @PathParam("person") String person,
    	    @ApiParam(value = "profile" ,required=true ) UserProfileEdit profile,
    	    @ApiParam(value = "Password, leave empty if you don't want to set any" ,required=false )@QueryParam("password") String password,
    		@Context HttpServletRequest req) {

    	try {
    		
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	User result = PersonDao.createPerson(repoDao, person,password, profile).asPerson();
	    	
	    	return Response.status(Response.Status.OK).entity(result).build();
	    	
    	} catch (DAOValidationException t) {
    		
    		logger.warn(t.getMessage(), t);
    		return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorResponse(t)).build();
    		
    	} catch (DAOSecurityException t) {
    		
    		logger.warn(t.getMessage(), t);
    		return Response.status(Response.Status.FORBIDDEN).entity(new ErrorResponse(t)).build();
    		
    	} catch (DAOMissingException t) {
    		
    		logger.warn(t.getMessage(), t);
    		return Response.status(Response.Status.NOT_FOUND).entity(new ErrorResponse(t)).build();
    		
    	} catch (Throwable t) {
    		
    		logger.error(t.getMessage(), t);
    		return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse(t)).build();
    	}
    }

    @PUT
    @Path("/people/{repository}/{person}/status/{status}")
    @ApiOperation(
    	value = "update the user status.",
    	notes = "update the user status. (admin rights are required.)")

    @ApiResponses(
    	value = {
			@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Void.class),
			@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
			@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
			@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
			@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
			@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class)
		})

    public Response updateUserStatus(
    		@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID,required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
    		@ApiParam(value = "username",required=true) @PathParam("person") String person,
    		@ApiParam(value = "the new status to set",required=true) @PathParam("status") PersonLifecycleService.PersonStatus status,
    		@ApiParam(value = "notify the user via mail",required=true,defaultValue = "true") @QueryParam("notify") Boolean notifyMail,
    		@Context HttpServletRequest req) {

    	try {
    		
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	PersonDao personDao = PersonDao.getPerson(repoDao, person);

	    	personDao.setStatus(status,notifyMail==null ? true : notifyMail);
	    	
	    	return Response.status(Response.Status.OK).build();
	    	
    	} catch (DAOValidationException t) {
    		
    		logger.warn(t.getMessage(), t);
    		return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorResponse(t)).build();
    		
    	} catch (DAOSecurityException t) {
    		
    		logger.warn(t.getMessage(), t);
    		return Response.status(Response.Status.FORBIDDEN).entity(new ErrorResponse(t)).build();
    		
    	} catch (DAOMissingException t) {
    		
    		logger.warn(t.getMessage(), t);
    		return Response.status(Response.Status.NOT_FOUND).entity(new ErrorResponse(t)).build();
    		
    	} catch (Throwable t) {
    		
    		logger.error(t.getMessage(), t);
    		return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse(t)).build();
    	}
    }

	@DELETE

	@Path("/people/{repository}/{person}")

	@ApiOperation(
			value = "Delete the user.",
			notes = "Delete the user. (admin rights are required.)")

	@ApiResponses(
			value = {
					@ApiResponse(code = 200, message = "OK.", response = Void.class),
					@ApiResponse(code = 400, message = "Preconditions are not present.", response = ErrorResponse.class),
					@ApiResponse(code = 401, message = "Authorization failed.", response = ErrorResponse.class),
					@ApiResponse(code = 403, message = "Session user has insufficient rights to perform this operation.", response = ErrorResponse.class),
					@ApiResponse(code = 404, message = "Ressources are not found.", response = ErrorResponse.class),
					@ApiResponse(code = 500, message = "Fatal error occured.", response = ErrorResponse.class)
			})

	public Response deleteUser(
			@ApiParam(value = "ID of repository (or \"-home-\" for home repository)",required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
			@ApiParam(value = "username",required=true) @PathParam("person") String person,
			@ApiParam(value = "force the deletion (if false then only persons which are previously marked for deletion are getting deleted)",required=false, defaultValue = "false") @QueryParam("force") Boolean force,
			@Context HttpServletRequest req) {

		try {

			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			PersonDao personDao = PersonDao.getPerson(repoDao, person);

			personDao.delete(force != null && force);

			return Response.status(Response.Status.OK).build();

		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}

    @OPTIONS        
    @Path("/people/{repository}/{person}")
    @ApiOperation(hidden = true, value = "")

    public Response options02() {
    	
    	return Response.status(Response.Status.OK).header("Allow", "OPTIONS, GET, POST, DELETE").build();
    }

    @PUT

    @Path("/people/{repository}/{person}/profile")    
    
    @ApiOperation(
    	value = "Set profile of the user.", 
    	notes = "Set profile of the user. (To set foreign profiles, admin rights are required.)")

    @ApiResponses(
    	value = { 
	        @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Void.class),
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class)
	    })

    public Response changeUserProfile(
    		@ApiParam(value = "ID of repository (or \"-home-\" for home repository)",required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
    		@ApiParam(value = "username (or \"-me-\" for current user)",required=true, defaultValue="-me-" ) @PathParam("person") String person,
    	    @ApiParam(value = "properties" ,required=true ) UserProfileEdit profile,
    		@Context HttpServletRequest req) {

    	try {
    		
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	PersonDao personDao = PersonDao.getPerson(repoDao, person);
	    	
	    	personDao.changeProfile(profile);
	    		    	
	    	return Response.status(Response.Status.OK).build();

    	} catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}
    }
    @PUT
    @Path("/people/{repository}/{person}/avatar")
    @ApiOperation(
    	value = "Set avatar of the user.",
    	notes = "Set avatar of the user. (To set foreign avatars, admin rights are required.)")
    @ApiResponses(
    	value = {
	        @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Void.class),
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class)
	    })

    public Response changeUserAvatar(
    		@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID,required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
    		@ApiParam(value = "username (or \"-me-\" for current user)",required=true, defaultValue="-me-" ) @PathParam("person") String person,
    	    @ApiParam(value = "avatar image" ,required=true ) @FormDataParam("avatar") InputStream avatar,
    		@Context HttpServletRequest req) {

    	try {

	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	PersonDao personDao = PersonDao.getPerson(repoDao, person);

	    	personDao.changeAvatar(avatar);

	    	return Response.status(Response.Status.OK).build();

    	} catch (Throwable t) {

    		return ErrorResponse.createResponse(t);
    	}
    }
    @DELETE
    @Path("/people/{repository}/{person}/avatar")
    @ApiOperation(
    	value = "Remove avatar of the user.",
    	notes = "Remove avatar of the user. (To Remove foreign avatars, admin rights are required.)")
    @ApiResponses(
    	value = {
	        @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Void.class),
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class)
	    })

    public Response removeUserAvatar(
    		@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID,required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
    		@ApiParam(value = "username (or \"-me-\" for current user)",required=true, defaultValue="-me-" ) @PathParam("person") String person,
    		@Context HttpServletRequest req) {

    	try {
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	PersonDao personDao = PersonDao.getPerson(repoDao, person);
	    	personDao.removeAvatar();
	    	return Response.status(Response.Status.OK).build();

    	} catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}
    }

    @OPTIONS        
    @Path("/people/{repository}/{person}/profile")
    @ApiOperation(hidden = true, value = "")

    public Response options03() {
    	
    	return Response.status(Response.Status.OK).header("Allow", "OPTIONS, PUT").build();
    }

    @PUT

    @Path("/people/{repository}/{person}/credential")    
    
    @ApiOperation(
    	value = "Change/Set password of the user.", 
    	notes = "Change/Set password of the user. (To change foreign passwords or set passwords, admin rights are required.)")

    @ApiResponses(
    	value = { 
	        @ApiResponse(code = 200, message = "OK.", response = Void.class),        
	        @ApiResponse(code = 400, message = "Preconditions are not present.", response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = "Authorization failed.", response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = "Session user has insufficient rights to perform this operation.", response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = "Ressources are not found.", response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = "Fatal error occured.", response = ErrorResponse.class) 
	    })

    public Response changeUserPassword(
    		@ApiParam(value = "ID of repository (or \"-home-\" for home repository)",required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
    		@ApiParam(value = "username (or \"-me-\" for current user)",required=true, defaultValue="-me-" ) @PathParam("person") String person,
    	    @ApiParam(value = "credential" ,required=true ) UserCredential credential,
    		@Context HttpServletRequest req) {

    	try {
    		
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	PersonDao personDao = PersonDao.getPerson(repoDao, person);
	    	
    		personDao.changePassword(credential.getOldPassword(), credential.getNewPassword());
	    		    	
	    	return Response.status(Response.Status.OK).build();
	    	
    	} catch (DAOValidationException t) {
    		
    		logger.warn(t.getMessage(), t);
    		return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorResponse(t)).build();
    		
    	} catch (DAOSecurityException t) {
    		
    		logger.warn(t.getMessage(), t);
    		return Response.status(Response.Status.FORBIDDEN).entity(new ErrorResponse(t)).build();
    		
    	} catch (DAOMissingException t) {
    		
    		logger.warn(t.getMessage(), t);
    		return Response.status(Response.Status.NOT_FOUND).entity(new ErrorResponse(t)).build();
    		
    	} catch (Throwable t) {
    		
    		logger.error(t.getMessage(), t);
    		return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse(t)).build();
    	}
    }

    @OPTIONS        
    @Path("/people/{repository}/{person}/credential")
    @ApiOperation(hidden = true, value = "")

    public Response options04() {	
    	
    	return Response.status(Response.Status.OK).header("Allow", "OPTIONS, PUT").build();
    }
    
    @GET

    @Path("/groups/{repository}")    
    
    @ApiOperation(
    	value = "Search groups.", 
    	notes = "Search groups. (admin rights are required.)")

    @ApiResponses(
        	value = { 
    	        @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = GroupEntries.class),
    	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
    	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
    	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
    	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
    	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
    	    })

        public Response searchGroups(
        		@ApiParam(value = "ID of repository (or \"-home-\" for home repository)",required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
        		@ApiParam(value = "pattern",required=true) @QueryParam("pattern") String pattern,
        		@ApiParam(value = "find a specific groupType",required=false) @QueryParam("groupType") String groupType,
				@ApiParam(value = "find a specific signupMethod for groups (or asterisk for all including one)",required=false) @QueryParam("signupMethod") String signupMethod,
				@ApiParam(value = "global search context, defaults to true, otherwise just searches for groups within the organizations",required=false,defaultValue="true") @QueryParam("global") Boolean global,
        		@ApiParam(value = RestConstants.MESSAGE_MAX_ITEMS, defaultValue=""+RestConstants.DEFAULT_MAX_ITEMS) @QueryParam("maxItems") Integer maxItems,
        	    @ApiParam(value = RestConstants.MESSAGE_SKIP_COUNT, defaultValue="0") @QueryParam("skipCount") Integer skipCount,
        	    @ApiParam(value = RestConstants.MESSAGE_SORT_PROPERTIES) @QueryParam("sortProperties") List<String> sortProperties,
        	    @ApiParam(value = RestConstants.MESSAGE_SORT_ASCENDING) @QueryParam("sortAscending") List<Boolean> sortAscending,
        		@Context HttpServletRequest req) {

        	try {
        		HashMap<String, String> props = new HashMap<String,String>();
        		if(groupType!=null && !groupType.isEmpty()){
        			props.put(CCConstants.getValidLocalName(CCConstants.CCM_PROP_GROUPEXTENSION_GROUPTYPE), groupType);
        		}
				if(signupMethod!=null && !signupMethod.isEmpty()){
					props.put(CCConstants.getValidLocalName(CCConstants.CCM_PROP_GROUP_SIGNUP_METHOD), signupMethod);
				}
    	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
    	    	SearchResult<String> search=SearchServiceFactory.getSearchService(repoDao.getId()).findAuthorities(
    	    					AuthorityType.GROUP,
    	    					pattern,
    	    					global==null ? true : global, 
    	    					skipCount!=null ? skipCount : 0,
    	    					maxItems!=null ? maxItems : RestConstants.DEFAULT_MAX_ITEMS,
    	    					new SortDefinition(sortProperties,sortAscending),
    	    					props
    	    			);

    	    	List<Group> result = new ArrayList<Group>();
    	    	for (String group: search.getData()) {
    	    		result.add(new GroupDao(repoDao,group).asGroup());
    	    	}	
    	    	GroupEntries response = new GroupEntries();
    	    	response.setList(result);
    	    	response.setPagination(new Pagination(search));		    	
    	    	
    	    	return Response.status(Response.Status.OK).entity(response).build();
    	    	
        	 
        	} catch (Throwable t) {
        		return ErrorResponse.createResponse(t);
        	}
    }

        
    @OPTIONS        
    @Path("/groups/{repository}")
    @ApiOperation(hidden = true, value = "")

    public Response options05() {
    	
    	return Response.status(Response.Status.OK).header("Allow", "OPTIONS, GET").build();
    }
    
    @GET

    @Path("/groups/{repository}/{group}")    
    
    @ApiOperation(
    	value = "Get the group.", 
    	notes = "Get the group. (To get foreign profiles, admin rights are required.)")

    @ApiResponses(
    	value = { 
	        @ApiResponse(code = 200, message = "OK.", response = GroupEntry.class),        
	        @ApiResponse(code = 400, message = "Preconditions are not present.", response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = "Authorization failed.", response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = "Session user has insufficient rights to perform this operation.", response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = "Ressources are not found.", response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = "Fatal error occured.", response = ErrorResponse.class) 
	    })

    public Response getGroup(
    		@ApiParam(value = "ID of repository (or \"-home-\" for home repository)",required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
    		@ApiParam(value = "groupname",required=true) @PathParam("group") String group,
    		@Context HttpServletRequest req) {

    	try {
    		
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	GroupDao groupDao = GroupDao.getGroup(repoDao, group);
	    	
	    	GroupEntry response = new GroupEntry();
	    	response.setGroup(groupDao.asGroup());
	    		    	
	    	return Response.status(Response.Status.OK).entity(response).build();
	    	
    	} catch (Throwable t) {
    		return ErrorResponse.createResponse(t, ErrorResponse.ErrorResponseLogging.relaxed);
    	}
    }
    
    @POST

    @Path("/groups/{repository}/{group}")    
    
    @ApiOperation(
    	value = "Create a new group.", 
    	notes = "Create a new group. (admin rights are required.)")

    @ApiResponses(
    	value = { 
	        @ApiResponse(code = 200, message = "OK.", response = Group.class),
	        @ApiResponse(code = 400, message = "Preconditions are not present.", response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = "Authorization failed.", response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = "Session user has insufficient rights to perform this operation.", response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = "Ressources are not found.", response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = "Fatal error occured.", response = ErrorResponse.class) 
	    })

    public Response createGroup(
    		@ApiParam(value = "ID of repository (or \"-home-\" for home repository)",required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
    		@ApiParam(value = "groupname",required=true) @PathParam("group") String group,
    		@ApiParam(value = "parent (will be added to this parent, also for name hashing), may be null",required=false) @QueryParam("parent") String parent,
    		@ApiParam(value = "properties" ,required=true ) GroupProfile profile,
    		@Context HttpServletRequest req) {

    	try {
    		
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	Group groupResult = GroupDao.createGroup(repoDao, group, profile,parent).asGroup();
	    	
	    	return Response.status(Response.Status.OK).entity(groupResult).build();
	    	
    	} catch (DAOValidationException t) {
    		
    		logger.warn(t.getMessage(), t);
    		return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorResponse(t)).build();
    		
    	} catch (DAOSecurityException t) {
    		
    		logger.warn(t.getMessage(), t);
    		return Response.status(Response.Status.FORBIDDEN).entity(new ErrorResponse(t)).build();
    		
    	} catch (DAOMissingException t) {
    		
    		logger.warn(t.getMessage(), t);
    		return Response.status(Response.Status.NOT_FOUND).entity(new ErrorResponse(t)).build();
    		
    	} catch (Throwable t) {
    		
    		logger.error(t.getMessage(), t);
    		return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse(t)).build();
    	}
    }
    
    @DELETE

    @Path("/groups/{repository}/{group}")    
    
    @ApiOperation(
    	value = "Delete the group.", 
    	notes = "Delete the group. (admin rights are required.)")

    @ApiResponses(
    	value = { 
	        @ApiResponse(code = 200, message = "OK.", response = Void.class),        
	        @ApiResponse(code = 400, message = "Preconditions are not present.", response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = "Authorization failed.", response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = "Session user has insufficient rights to perform this operation.", response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = "Ressources are not found.", response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = "Fatal error occured.", response = ErrorResponse.class) 
	    })

    public Response deleteGroup(
    		@ApiParam(value = "ID of repository (or \"-home-\" for home repository)",required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
    		@ApiParam(value = "groupname",required=true) @PathParam("group") String group,
    		@Context HttpServletRequest req) {

    	try {
			new MCAlfrescoAPIClient().doInTransaction(()-> {
				RepositoryDao repoDao = RepositoryDao.getRepository(repository);
				GroupDao groupDao = GroupDao.getGroup(repoDao, group);

				groupDao.delete();
				return groupDao;
			});
	    	return Response.status(Response.Status.OK).build();
	    	
    	} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
    }

    @OPTIONS        
    @Path("/groups/{repository}/{group}")
    @ApiOperation(hidden = true, value = "")

    public Response options06() {
    	
    	return Response.status(Response.Status.OK).header("Allow", "OPTIONS, GET, POST, DELETE").build();
    }

    @PUT

    @Path("/groups/{repository}/{group}/profile")    
    
    @ApiOperation(
    	value = "Set profile of the group.", 
    	notes = "Set profile of the group. (admin rights are required.)")

    @ApiResponses(
    	value = { 
	        @ApiResponse(code = 200, message = "OK.", response = Void.class),        
	        @ApiResponse(code = 400, message = "Preconditions are not present.", response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = "Authorization failed.", response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = "Session user has insufficient rights to perform this operation.", response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = "Ressources are not found.", response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = "Fatal error occured.", response = ErrorResponse.class) 
	    })

    public Response changeGroupProfile(
    		@ApiParam(value = "ID of repository (or \"-home-\" for home repository)",required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
    		@ApiParam(value = "groupname",required=true ) @PathParam("group") String group,
    	    @ApiParam(value = "properties" ,required=true ) GroupProfile profile,
    		@Context HttpServletRequest req) {

    	try {
			new MCAlfrescoAPIClient().doInTransaction(()->{
				RepositoryDao repoDao = RepositoryDao.getRepository(repository);
				GroupDao groupDao = GroupDao.getGroup(repoDao, group);
				groupDao.changeProfile(profile);
				return groupDao;
			});
			return Response.status(Response.Status.OK).build();
    	} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
    	}
    }

    @OPTIONS        
    @Path("/groups/{repository}/{group}/profile")
    @ApiOperation(hidden = true, value = "")

    public Response options07() {
    	
    	return Response.status(Response.Status.OK).header("Allow", "OPTIONS, PUT").build();
    }

	@POST
	@Path("/groups/{repository}/{group}/signup/config")

	@ApiOperation(
			value = "set group signup options",
			notes =" requires admin rights"
	)

	@ApiResponses(
			value = {
					@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Void.class),
					@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
					@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
					@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
					@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
					@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class)
			})

	public Response signupGroupDetails(
			@ApiParam(value = "ID of repository (or \"-home-\" for home repository)",required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
			@ApiParam(value = "ID of group",required=true) @PathParam("group") String group,
			@ApiParam(value = "Details to edit",required=true) GroupSignupDetails details,
			@Context HttpServletRequest req) {

		try {

			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			GroupDao.getGroup(repoDao,group).setSignup(details);
			return Response.status(Response.Status.OK).build();

		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}

	}

	@POST
	@Path("/groups/{repository}/{group}/signup")

	@ApiOperation(
			value = "let the current user signup to the given group"
	)

	@ApiResponses(
			value = {
					@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = GroupSignupResult.class),
					@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
					@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
					@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
					@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
					@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class)
			})

	public Response signupGroup(
			@ApiParam(value = "ID of repository (or \"-home-\" for home repository)",required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
			@ApiParam(value = "ID of group",required=true) @PathParam("group") String group,
			@ApiParam(value = "Password for signup (only required if signupMethod == password)",required=false) @QueryParam("password") String password,
			@Context HttpServletRequest req) {

		try {

			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			return Response.status(Response.Status.OK).entity(GroupDao.getGroup(repoDao,group).signupUser(password)).build();

		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}

	}

	@GET
	@Path("/groups/{repository}/{group}/signup/list")

	@ApiOperation(
			value = "list pending users that want to join this group",
			notes = "Requires admin rights or org administrator on this group"
	)

	@ApiResponses(
			value = {
					@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = User[].class),
					@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
					@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
					@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
					@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
					@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class)
			})

	public Response signupGroupList(
			@ApiParam(value = "ID of repository (or \"-home-\" for home repository)",required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
			@ApiParam(value = "ID of group",required=true) @PathParam("group") String group,
			@Context HttpServletRequest req) {

		try {

			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			List<User> persons = GroupDao.getGroup(repoDao, group).signupUserList().stream().map(personDao -> {
				try {
					return personDao.asPerson();
				} catch (DAOException e) {
					throw new RuntimeException(e);
				}
			}).collect(Collectors.toList());
			return Response.status(Response.Status.OK).entity(persons).build();

		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}

	}

	@PUT
	@Path("/groups/{repository}/{group}/signup/list/{user}")

	@ApiOperation(
			value = "put the pending user into the group",
			notes = "Requires admin rights or org administrator on this group"
	)

	@ApiResponses(
			value = {
					@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = void.class),
					@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
					@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
					@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
					@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
					@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class)
			})

	public Response confirmSignup(
			@ApiParam(value = "ID of repository (or \"-home-\" for home repository)",required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
			@ApiParam(value = "ID of group",required=true) @PathParam("group") String group,
			@ApiParam(value = "ID of user",required=true) @PathParam("user") String user,
			@Context HttpServletRequest req) {

		try {

			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			GroupDao.getGroup(repoDao, group).confirmSignup(user);
			return Response.status(Response.Status.OK).build();

		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}

	}

	@DELETE
	@Path("/groups/{repository}/{group}/signup/list/{user}")

	@ApiOperation(
			value = "reject the pending user",
			notes = "Requires admin rights or org administrator on this group"
	)

	@ApiResponses(
			value = {
					@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = void.class),
					@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
					@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
					@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
					@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
					@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class)
			})

	public Response rejectSignup(
			@ApiParam(value = "ID of repository (or \"-home-\" for home repository)",required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
			@ApiParam(value = "ID of group",required=true) @PathParam("group") String group,
			@ApiParam(value = "ID of user",required=true) @PathParam("user") String user,
			@Context HttpServletRequest req) {

		try {

			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			GroupDao.getGroup(repoDao, group).rejectSignup(user);
			return Response.status(Response.Status.OK).build();

		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}

	}

	@GET

    @Path("/people/{repository}/{person}/memberships")

    @ApiOperation(
    	value = "Get all groups the given user is member of."
    	)

    @ApiResponses(
    	value = { 
	        @ApiResponse(code = 200, message = "OK.", response = GroupEntries.class),
	        @ApiResponse(code = 400, message = "Preconditions are not present.", response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = "Authorization failed.", response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = "Session user has insufficient rights to perform this operation.", response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = "Ressources are not found.", response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = "Fatal error occured.", response = ErrorResponse.class) 
	    })

    public Response getUserGroups(
    		@ApiParam(value = "ID of repository (or \"-home-\" for home repository)",required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
    		@ApiParam(value = "authority name",required=true ) @PathParam("person") String person,
       		@ApiParam(value = "pattern",required=false) @QueryParam("pattern") String pattern,
    		@ApiParam(value = RestConstants.MESSAGE_MAX_ITEMS, defaultValue=""+RestConstants.DEFAULT_MAX_ITEMS) @QueryParam("maxItems") Integer maxItems,
    	    @ApiParam(value = RestConstants.MESSAGE_SKIP_COUNT, defaultValue="0") @QueryParam("skipCount") Integer skipCount,
    	    @ApiParam(value = RestConstants.MESSAGE_SORT_PROPERTIES) @QueryParam("sortProperties") List<String> sortProperties,
    	    @ApiParam(value = RestConstants.MESSAGE_SORT_ASCENDING) @QueryParam("sortAscending") List<Boolean> sortAscending,

    		@Context HttpServletRequest req) {

    	try {

    		RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			GroupEntries response = PersonDao.getPerson(repoDao, person).getMemberships(
    				pattern, 
    				skipCount!=null ? skipCount : 0,
	    			maxItems!=null ? maxItems : RestConstants.DEFAULT_MAX_ITEMS,
					new SortDefinition(sortProperties,sortAscending)
			);

	    	return Response.status(Response.Status.OK).entity(response).build();

    	} catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}
    }


    @GET

    @Path("/groups/{repository}/{group}/members")    
    
    @ApiOperation(
    	value = "Get all members of the group.", 
    	notes = "Get all members of the group. (admin rights are required.)")

    @ApiResponses(
    	value = { 
	        @ApiResponse(code = 200, message = "OK.", response = AuthorityEntries.class),        
	        @ApiResponse(code = 400, message = "Preconditions are not present.", response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = "Authorization failed.", response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = "Session user has insufficient rights to perform this operation.", response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = "Ressources are not found.", response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = "Fatal error occured.", response = ErrorResponse.class) 
	    })

    public Response getMembership(
    		@ApiParam(value = "ID of repository (or \"-home-\" for home repository)",required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
    		@ApiParam(value = "authority name (begins with GROUP_)",required=true ) @PathParam("group") String group,
       		@ApiParam(value = "pattern",required=false) @QueryParam("pattern") String pattern,
       		@ApiParam(value = "authorityType either GROUP or USER, empty to show all",required=false) @QueryParam("authorityType") String authorityType,
    		@ApiParam(value = RestConstants.MESSAGE_MAX_ITEMS, defaultValue=""+RestConstants.DEFAULT_MAX_ITEMS) @QueryParam("maxItems") Integer maxItems,
    	    @ApiParam(value = RestConstants.MESSAGE_SKIP_COUNT, defaultValue="0") @QueryParam("skipCount") Integer skipCount,
    	    @ApiParam(value = RestConstants.MESSAGE_SORT_PROPERTIES) @QueryParam("sortProperties") List<String> sortProperties,
    	    @ApiParam(value = RestConstants.MESSAGE_SORT_ASCENDING) @QueryParam("sortAscending") List<Boolean> sortAscending,
 
    		@Context HttpServletRequest req) {

    	try {
    		
    		RepositoryDao repoDao = RepositoryDao.getRepository(repository);
    		GroupDao.getGroup(repoDao, group).checkAdminAccess();
	    	SearchResult<String> search=SearchServiceFactory.getSearchService(repoDao.getId()).searchGroupMembers(
	    					group,
	    					pattern,
	    					authorityType,
	    					skipCount!=null ? skipCount : 0,
	    	    			maxItems!=null ? maxItems : RestConstants.DEFAULT_MAX_ITEMS,
	    					new SortDefinition(sortProperties,sortAscending)
	    			);
	    	AuthorityEntries response = new AuthorityEntries();
    		List<Authority> result = new ArrayList<Authority>();
	    	for (String member: search.getData()) {
	    		result.add(
	    				member.startsWith(PermissionService.GROUP_PREFIX) ?
	    							new GroupDao(repoDao,member).asGroup() :
	    							new PersonDao(repoDao, member).asPerson());	
	    	}	
	    	response.setList(result);
	    	response.setPagination(new Pagination(search));
	    	return Response.status(Response.Status.OK).entity(response).build();
	    	
    	} catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}
    }


	@GET

	@Path("/groups/{repository}/{group}/type/{type}")

	@ApiOperation(
			value = "Get a subgroup by the specified type",
			notes = "Get a subgroup by the specified type")

	@ApiResponses(
			value = {
					@ApiResponse(code = 200, message = "OK.", response = AuthorityEntries.class),
					@ApiResponse(code = 400, message = "Preconditions are not present.", response = ErrorResponse.class),
					@ApiResponse(code = 401, message = "Authorization failed.", response = ErrorResponse.class),
					@ApiResponse(code = 403, message = "Session user has insufficient rights to perform this operation.", response = ErrorResponse.class),
					@ApiResponse(code = 404, message = "Ressources are not found.", response = ErrorResponse.class),
					@ApiResponse(code = 500, message = "Fatal error occured.", response = ErrorResponse.class)
			})

	public Response getSubgroupByType(
			@ApiParam(value = "ID of repository (or \"-home-\" for home repository)",required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
			@ApiParam(value = "authority name of the parent/primary group (begins with GROUP_)",required=true ) @PathParam("group") String group,
			@ApiParam(value = "authorityType either GROUP or USER, empty to show all",required=true) @PathParam("type") String type,

			@Context HttpServletRequest req) {

		try {

			RepositoryDao repoDao = RepositoryDao.getRepository(repository);

			GroupEntry response = new GroupEntry();
			response.setGroup( GroupDao.getGroup(repoDao, group).getSubgroupByType(type).asGroup());

			return Response.status(Response.Status.OK).entity(response).build();

		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}

	@PUT

    @Path("/groups/{repository}/{group}/members/{member}")    
    
    @ApiOperation(
    	value = "Add member to the group.", 
    	notes = "Add member to the group. (admin rights are required.)")

    @ApiResponses(
    	value = { 
	        @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Void.class),        
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
	        @ApiResponse(code = 409, message = RestConstants.HTTP_409, response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })

    public Response addMembership(
    		@ApiParam(value = "ID of repository (or \"-home-\" for home repository)",required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
    		@ApiParam(value = "groupname",required=true ) @PathParam("group") String group,
    		@ApiParam(value = "authorityName of member",required=true ) @PathParam("member") String member,
    		@Context HttpServletRequest req) {

    	try {
			new MCAlfrescoAPIClient().doInTransaction(()-> {
				RepositoryDao repoDao = RepositoryDao.getRepository(repository);
				GroupDao groupDao = GroupDao.getGroup(repoDao, group);
				groupDao.addMember(member);
				return groupDao;
			});
	    	return Response.status(Response.Status.OK).build();
	    	
    	} catch (Throwable t) {
    		return  ErrorResponse.createResponse(t);
    	}
    }
    
    @DELETE

    @Path("/groups/{repository}/{group}/members/{member}")    
    
    @ApiOperation(
    	value = "Delete member from the group.", 
    	notes = "Delete member from the group. (admin rights are required.)")

    @ApiResponses(
    	value = { 
	        @ApiResponse(code = 200, message = "OK.", response = Void.class),        
	        @ApiResponse(code = 400, message = "Preconditions are not present.", response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = "Authorization failed.", response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = "Session user has insufficient rights to perform this operation.", response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = "Ressources are not found.", response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = "Fatal error occured.", response = ErrorResponse.class) 
	    })

    public Response deleteMembership(
    		@ApiParam(value = "ID of repository (or \"-home-\" for home repository)",required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
    		@ApiParam(value = "groupname",required=true ) @PathParam("group") String group,
    		@ApiParam(value = "authorityName of member",required=true ) @PathParam("member") String member,
    		@Context HttpServletRequest req) {

    	try {
			new MCAlfrescoAPIClient().doInTransaction(()-> {
				RepositoryDao repoDao = RepositoryDao.getRepository(repository);
				GroupDao groupDao = GroupDao.getGroup(repoDao, group);
				groupDao.deleteMember(member);
				return groupDao;
			});
	    	return Response.status(Response.Status.OK).build();
    	} catch (Throwable t) {
			return  ErrorResponse.createResponse(t);
		}
    }

    @OPTIONS        
    @Path("/groups/{repository}/{group}/members/{member}")
    @ApiOperation(hidden = true, value = "")

    public Response options09() {
    	
    	return Response.status(Response.Status.OK).header("Allow", "OPTIONS, PUT, DELETE").build();
    }
    
    
    @GET

    @Path("/authorities/{repository}")    
    
    @ApiOperation(
    	value = "Search authorities.", 
    	notes = "Search authorities.")

    @ApiResponses(
    	value = { 
	        @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = AuthorityEntries.class),        
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })

    public Response searchAuthorities(
    		@ApiParam(value = "ID of repository (or \"-home-\" for home repository)",required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
    		@ApiParam(value = "pattern",required=true) @QueryParam("pattern") String pattern,
    		@ApiParam(value = "global search context, defaults to true, otherwise just searches for users within the organizations",required=false,defaultValue="true") @QueryParam("global") Boolean global,
			@ApiParam(value = "find a specific groupType (does nothing for persons)",required=false) @QueryParam("groupType") String groupType,
			@ApiParam(value = "find a specific signupMethod for groups (or asterisk for all including one) (does nothing for persons)",required=false) @QueryParam("signupMethod") String signupMethod,
			@ApiParam(value = RestConstants.MESSAGE_MAX_ITEMS, defaultValue=""+RestConstants.DEFAULT_MAX_ITEMS) @QueryParam("maxItems") Integer maxItems,
    	    @ApiParam(value = RestConstants.MESSAGE_SKIP_COUNT, defaultValue="0") @QueryParam("skipCount") Integer skipCount,
    		@Context HttpServletRequest req) {

    	try {
			HashMap<String, String> props = new HashMap<>();
			if(groupType!=null && !groupType.isEmpty()){
				props.put(CCConstants.getValidLocalName(CCConstants.CCM_PROP_GROUPEXTENSION_GROUPTYPE), groupType);
			}
			if(signupMethod!=null && !signupMethod.isEmpty()){
				props.put(CCConstants.getValidLocalName(CCConstants.CCM_PROP_GROUP_SIGNUP_METHOD), signupMethod);
			}
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	SearchResult<String> search=SearchServiceFactory.getSearchService(repoDao.getId()).findAuthorities(
	    					null,
	    					pattern,
	    					global==null ? true : global,
	    					skipCount!=null ? skipCount : 0,
	    	    			maxItems!=null ? maxItems : RestConstants.DEFAULT_MAX_ITEMS,
	    					null,
	    					props
	    					
	    			);

	    	List<Authority> result = new ArrayList<Authority>();
	    	for (String user: search.getData()) {
	    		result.add(getUserOrGroup(repoDao, user));
	    	}	
	    	AuthorityEntries response = new AuthorityEntries();
	    	response.setList(result);
	    	response.setPagination(new Pagination(search));
	    		    	
	    	
	    	return Response.status(Response.Status.OK).entity(response).build();
	    	
    	 
    	} catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}
  }
	@GET

	@Path("/authorities/{repository}/recent")

	@ApiOperation(
			value = "Get recently invited authorities.",
			notes = "Get the authorities the current user has recently invited.")

	@ApiResponses(
			value = {
					@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = AuthorityEntries.class),
					@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
					@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
					@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
					@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
					@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class)
			})

	public Response getRecentlyInvited(
			@ApiParam(value = "ID of repository (or \"-home-\" for home repository)",required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
			@Context HttpServletRequest req) {

		try {
			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			List<String> recent = PermissionServiceFactory.getPermissionService(repoDao.getId()).getRecentlyInvited();
			List<Authority> result = new ArrayList<Authority>();
			for (String user: recent) {
				result.add(getUserOrGroup(repoDao, user));
			}
			AuthorityEntries response = new AuthorityEntries();
			response.setList(result);
			return Response.status(Response.Status.OK).entity(response).build();


		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}
	private Authority getUserOrGroup(RepositoryDao repoDao, String authority) throws DAOException {

		if(authority.startsWith(AuthorityType.GROUP.getPrefixString())){
			return new GroupDao(repoDao,authority).asGroup();
		}else{
			return new PersonDao(repoDao,authority).asPersonSimple();
		}
	}
}

