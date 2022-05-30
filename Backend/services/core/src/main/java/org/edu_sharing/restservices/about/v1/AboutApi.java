package org.edu_sharing.restservices.about.v1;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.server.RepoFactory;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.restservices.ApiApplication;
import org.edu_sharing.restservices.ApiService;
import org.edu_sharing.restservices.RestConstants;
import org.edu_sharing.restservices.about.v1.model.About;
import org.edu_sharing.restservices.about.v1.model.Service;
import org.edu_sharing.restservices.about.v1.model.ServiceInstance;
import org.edu_sharing.restservices.about.v1.model.ServiceVersion;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.edu_sharing.service.mime.MimeTypesV2;
import org.edu_sharing.service.monitoring.Monitoring;
import org.edu_sharing.service.version.VersionService;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

@Path("/_about")
@Tag(name="ABOUT")
@Consumes({ "application/json" })
@Produces({"application/json"})
public class AboutApi  {

	private static Logger logger = Logger.getLogger(AboutApi.class);

    @GET
    
    @Operation(summary = "Discover the API.", description = "Get all services provided by this API.")
    
    @ApiResponses(
    	value = { 
			@ApiResponse(responseCode="200", description="OK.", content = @Content(schema = @Schema(implementation = About.class))), 
	        @ApiResponse(responseCode="401", description="Authorization failed.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="500", description="Fatal error occured.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))) 
    	})

    public Response about() {

    	try {
	    	About about = new About();
	    	
	    	ServiceVersion version = new ServiceVersion();
	    	
	    	version.setMajor(ApiApplication.MAJOR);
	    	version.setMinor(ApiApplication.MINOR);
	    	version.setRepository(VersionService.getVersionNoException(VersionService.Type.REPOSITORY));
	    	version.setRenderservice(VersionService.getVersionNoException(VersionService.Type.RENDERSERVICE));
	    	
	    	logger.debug("Request via domain "+ org.edu_sharing.alfresco.repository.server.authentication.Context.getCurrentInstance().getRequest().getServerName());
	
	    	about.setVersion(version);

	    	about.setLastCacheUpdate(RepoFactory.getLastRefreshed());
	    	
	    	about.setThemesUrl(new MimeTypesV2(ApplicationInfoList.getHomeRepository()).getThemePath());
	    	
	    	Map<String, Service> services = new HashMap<String, Service>(); 
	    	for (Class<?> clazz : ApiApplication.SERVICES) {
	
				ApiService rest = clazz.getAnnotation(ApiService.class);
				Path path = clazz.getAnnotation(Path.class);
	
				if (rest != null && path != null) {
	    			    			
					String name = rest.value();
					
	    			Service service = services.get(name);
	    			if (service == null) {
	    				
	    				services.put(name, service = new Service());    				
	
	    				service.setName(name);    				
	    				about.getServices().add(service);
	    			}
	    			
	    			ServiceInstance instance = new ServiceInstance();
	    			
	    			ServiceVersion v = new ServiceVersion();
	    			v.setMajor(rest.major());
	    			v.setMinor(rest.minor());
	    			
	    			instance.setVersion(v);
	    			
	    			instance.setEndpoint(path.value());
	    			
	    			service.getInstances().add(instance);    			
	    		}    		
	    	}
	    	
	    	
	    	return Response.status(Response.Status.OK).entity(about).build();
	    	
		} catch (Throwable t) {
			
			logger.error(t.getMessage(), t);			
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse(t)).build();
		}
    	
    }
    
    @OPTIONS        
    @Hidden
    public Response options() {
    	
    	return Response.status(Response.Status.OK).header("Allow", "OPTIONS, GET").build();
    }
    
    @GET
	@Path("/status/{mode}")
	@Operation(summary = "status of repo services", description = "returns http status 200 when ok")

	@ApiResponses(value = { @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = String.class))),
			@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })
	public Response status(@PathParam("mode") Monitoring.Modes mode, 
			@QueryParam("timeoutSeconds") @DefaultValue("10") int timeout,
			@Context HttpServletRequest req) {
    	
    	try {
    		String result = null;
	    	if(Monitoring.Modes.SERVICE.equals(mode)) {
	    		result = new Monitoring().alfrescoServicesCheckTimeout(timeout);
			}else {
				result = new Monitoring().alfrescoSearchEngineCheckTimeout(timeout);
			}
	    	logger.debug("result:" + result);
	    	//check if it is a node id?
	    	//NodeServiceFactory.getLocalService().exists(protocol, store, result)

			if(result == null){
				return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
			}
	    	
	    	return Response.ok().build();
	    	
		}catch(Throwable t) {
			logger.debug(t.getMessage(),t);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    	}
    	
    	
    }
    
    @OPTIONS
	@Path("/status/repo/{mode}")
	@Hidden
	public Response options2() {
		return Response.status(Response.Status.OK).header("Allow", "OPTIONS, GET").build();
	}
    
   
    
}

