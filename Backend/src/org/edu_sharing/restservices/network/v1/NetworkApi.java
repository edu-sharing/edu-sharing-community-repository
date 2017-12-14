package org.edu_sharing.restservices.network.v1;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.edu_sharing.restservices.ApiService;
import org.edu_sharing.restservices.DAOMissingException;
import org.edu_sharing.restservices.DAOSecurityException;
import org.edu_sharing.restservices.DAOValidationException;
import org.edu_sharing.restservices.RepositoryDao;
import org.edu_sharing.restservices.network.v1.model.RepoEntries;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.edu_sharing.restservices.shared.Repo;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Path("/network/v1")
@Api(tags = {"NETWORK v1"})
@ApiService(value="NETWORK", major=1, minor=0)
public class NetworkApi  {

	private static Logger logger = Logger.getLogger(NetworkApi.class);

    @GET
    @Path("/repositories")
        
    @ApiOperation(
    	value = "Get repositories.", 
    	notes = "Get repositories.")
    
    @ApiResponses(
    	value = { 
    		@ApiResponse(code = 200, message = "OK.", response = RepoEntries.class),        
	        @ApiResponse(code = 400, message = "Preconditions are not present.", response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = "Authorization failed.", response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = "Session user has insufficient rights to perform this operation.", response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = "Ressources are not found.", response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = "Fatal error occured.", response = ErrorResponse.class) 
    	})

    public Response getRepositories(
		@Context HttpServletRequest req) {
    	
    	try {
    			    		    	
	    	List<Repo> repos = new ArrayList<Repo>();
	    	for (RepositoryDao repository : RepositoryDao.getRepositories()) {
	    		
	    		Repo repo = new Repo();
	    		
	    		repo.setId(repository.getId());
	    		if(repository.isHomeRepo())
	    			repo.setId(RepositoryDao.HOME);
	    		repo.setTitle(repository.getCaption());
	    		repo.setIcon(repository.getIcon());
	    		repo.setLogo(repository.getLogo());
	    		repo.setHomeRepo(repository.isHomeRepo());
				repo.setRepositoryType(repository.getRepositoryType());
	    		
	    		repos.add(repo);
	    	}
	    	
	    	RepoEntries response = new RepoEntries();
	    	response.setList(repos);
	    	
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
    
    @OPTIONS    
    @Path("/repositories")
    @ApiOperation(hidden = true, value = "")

    public Response options() {
    	
    	return Response.status(Response.Status.OK).header("Allow", "OPTIONS, GET").build();
    }
    
}

