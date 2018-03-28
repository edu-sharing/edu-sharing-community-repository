package org.edu_sharing.restservices.iam.v1;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.restservices.ApiService;
import org.edu_sharing.restservices.DAOMissingException;
import org.edu_sharing.restservices.DAOSecurityException;
import org.edu_sharing.restservices.DAOValidationException;
import org.edu_sharing.restservices.GroupDao;
import org.edu_sharing.restservices.NodeDao;
import org.edu_sharing.restservices.PersonDao;
import org.edu_sharing.restservices.RepositoryDao;
import org.edu_sharing.restservices.RestConstants;
import org.edu_sharing.restservices.iam.v1.model.AuthorityEntries;
import org.edu_sharing.restservices.iam.v1.model.GroupEntries;
import org.edu_sharing.restservices.iam.v1.model.GroupEntry;
import org.edu_sharing.restservices.iam.v1.model.Preferences;
import org.edu_sharing.restservices.iam.v1.model.UserEntries;
import org.edu_sharing.restservices.iam.v1.model.UserEntry;
import org.edu_sharing.restservices.node.v1.model.NodeEntries;
import org.edu_sharing.restservices.shared.Authority;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.edu_sharing.restservices.shared.Filter;
import org.edu_sharing.restservices.shared.Group;
import org.edu_sharing.restservices.shared.GroupProfile;
import org.edu_sharing.restservices.shared.Node;
import org.edu_sharing.restservices.shared.NodeRef;
import org.edu_sharing.restservices.shared.Pagination;
import org.edu_sharing.restservices.shared.UserCredential;
import org.edu_sharing.restservices.shared.UserProfile;
import org.edu_sharing.restservices.shared.UserSimple;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
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
    		@ApiParam(value = RestConstants.MESSAGE_MAX_ITEMS, defaultValue=""+RestConstants.DEFAULT_MAX_ITEMS) @QueryParam("maxItems") Integer maxItems,
    	    @ApiParam(value = RestConstants.MESSAGE_SKIP_COUNT, defaultValue="0") @QueryParam("skipCount") Integer skipCount,
    	    @ApiParam(value = RestConstants.MESSAGE_SORT_PROPERTIES) @QueryParam("sortProperties") List<String> sortProperties,
    	    @ApiParam(value = RestConstants.MESSAGE_SORT_ASCENDING) @QueryParam("sortAscending") List<Boolean> sortAscending,
    		@Context HttpServletRequest req) {

    	try {
    		
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	SearchResult<String> search=SearchServiceFactory.getSearchService(repoDao.getId()).searchUsers(
	    					pattern,
	    					global==null ? true : global,
	    					skipCount!=null ? skipCount : 0,
	    	    			maxItems!=null ? maxItems : RestConstants.DEFAULT_MAX_ITEMS,
	    					new SortDefinition(CCConstants.NAMESPACE_CM,sortProperties,sortAscending),
	    					null
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
    	notes = "Get the user. (To get foreign profiles, admin rights are required.)")

    @ApiResponses(
    	value = { 
	        @ApiResponse(code = 200, message = "OK.", response = UserEntry.class),        
	        @ApiResponse(code = 400, message = "Preconditions are not present.", response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = "Authorization failed.", response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = "Session user has insufficient rights to perform this operation.", response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = "Ressources are not found.", response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = "Fatal error occured.", response = ErrorResponse.class) 
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
	    	
	    	org.edu_sharing.repository.server.authentication.Context context =  org.edu_sharing.repository.server.authentication.Context.getCurrentInstance();
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
	    		List<Node> nodes = NodeDao.sortAndFilterByType(repoDao,refList,sortDefinition,null,propFilter);
	    		result.setNodes(nodes);
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
    
    @POST

    @Path("/people/{repository}/{person}")    
    
    @ApiOperation(
    	value = "Create a new user.", 
    	notes = "Create a new user. (admin rights are required.)")

    @ApiResponses(
    	value = { 
	        @ApiResponse(code = 200, message = "OK.", response = Void.class),        
	        @ApiResponse(code = 400, message = "Preconditions are not present.", response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = "Authorization failed.", response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = "Session user has insufficient rights to perform this operation.", response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = "Ressources are not found.", response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = "Fatal error occured.", response = ErrorResponse.class) 
	    })

    public Response createUser(
    		@ApiParam(value = "ID of repository (or \"-home-\" for home repository)",required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
    		@ApiParam(value = "username",required=true) @PathParam("person") String person,
    	    @ApiParam(value = "profile" ,required=true ) UserProfile profile,
    	    @ApiParam(value = "Password, leave empty if you don't want to set any" ,required=false )@QueryParam("password") String password,
    		@Context HttpServletRequest req) {

    	try {
    		
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	PersonDao.createPerson(repoDao, person,password, profile);
	    	
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
    		@Context HttpServletRequest req) {

    	try {
    		
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	PersonDao personDao = PersonDao.getPerson(repoDao, person);

	    	personDao.delete();
	    	
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
    	    @ApiParam(value = "properties" ,required=true ) UserProfile profile,
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
    
    @POST

    @Path("/groups/{repository}/{group}")    
    
    @ApiOperation(
    	value = "Create a new group.", 
    	notes = "Create a new group. (admin rights are required.)")

    @ApiResponses(
    	value = { 
	        @ApiResponse(code = 200, message = "OK.", response = Void.class),        
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
	    	GroupDao.createGroup(repoDao, group, profile,parent);
	    	
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
    		
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	GroupDao groupDao = GroupDao.getGroup(repoDao, group);

	    	groupDao.delete();
	    	
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
    		
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	GroupDao groupDao = GroupDao.getGroup(repoDao, group);
	    	
	    	groupDao.changeProfile(profile);
	    		    	
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
    @Path("/groups/{repository}/{group}/profile")
    @ApiOperation(hidden = true, value = "")

    public Response options07() {
    	
    	return Response.status(Response.Status.OK).header("Allow", "OPTIONS, PUT").build();
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

    @OPTIONS        
    @Path("/groups/{repository}/{group}/members")
    @ApiOperation(hidden = true, value = "")

    public Response options08() {
    	
    	return Response.status(Response.Status.OK).header("Allow", "OPTIONS, GET").build();
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
    		
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	GroupDao groupDao = GroupDao.getGroup(repoDao, group);
	    	
	    	groupDao.addMember(member);
	    		    	
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
    		
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	GroupDao groupDao = GroupDao.getGroup(repoDao, group);
	    	
	    	groupDao.deleteMember(member);
	    		    	
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
    		@ApiParam(value = RestConstants.MESSAGE_MAX_ITEMS, defaultValue=""+RestConstants.DEFAULT_MAX_ITEMS) @QueryParam("maxItems") Integer maxItems,
    	    @ApiParam(value = RestConstants.MESSAGE_SKIP_COUNT, defaultValue="0") @QueryParam("skipCount") Integer skipCount,
    		@Context HttpServletRequest req) {

    	try {
    		
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	SearchResult<String> search=SearchServiceFactory.getSearchService(repoDao.getId()).findAuthorities(
	    					null,
	    					pattern,
	    					global==null ? true : global,
	    					skipCount!=null ? skipCount : 0,
	    	    			maxItems!=null ? maxItems : RestConstants.DEFAULT_MAX_ITEMS,
	    					null,
	    					null
	    					
	    			);

	    	List<Authority> result = new ArrayList<Authority>();
	    	for (String user: search.getData()) {
	    		
	    		if(user.startsWith(AuthorityType.GROUP.getPrefixString())){
	    			result.add(new GroupDao(repoDao,user).asGroup());
	    		}else{
	    			result.add(new PersonDao(repoDao,user).asPersonSimple());
	    		}
	    	
	    	}	
	    	AuthorityEntries response = new AuthorityEntries();
	    	response.setList(result);
	    	response.setPagination(new Pagination(search));
	    		    	
	    	
	    	return Response.status(Response.Status.OK).entity(response).build();
	    	
    	 
    	} catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}
  }
}

