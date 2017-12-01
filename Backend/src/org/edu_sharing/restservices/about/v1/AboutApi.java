package org.edu_sharing.restservices.about.v1;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.edu_sharing.repository.server.authentication.Context;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.restservices.ApiApplication;
import org.edu_sharing.restservices.ApiService;
import org.edu_sharing.restservices.about.v1.model.About;
import org.edu_sharing.restservices.about.v1.model.Service;
import org.edu_sharing.restservices.about.v1.model.ServiceInstance;
import org.edu_sharing.restservices.about.v1.model.ServiceVersion;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.edu_sharing.service.mime.MimeTypesV2;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Path("/_about")
@Api(tags = {"ABOUT"})
public class AboutApi  {

	private static Logger logger = Logger.getLogger(AboutApi.class);

    @GET
    
    @ApiOperation(
    	value = "Discover the API.", 
    	notes = "Get all services provided by this API.")
    
    @ApiResponses(
    	value = { 
			@ApiResponse(code = 200, message = "OK.", response = About.class), 
	        @ApiResponse(code = 401, message = "Authorization failed.", response = ErrorResponse.class),        
	        @ApiResponse(code = 500, message = "Fatal error occured.", response = ErrorResponse.class) 
    	})

    public Response about() {

    	try {
	    	About about = new About();
	    	
	    	ServiceVersion version = new ServiceVersion();
	    	
	    	version.setMajor(ApiApplication.MAJOR);
	    	version.setMinor(ApiApplication.MINOR);
	    	
	    	logger.info("Request via domain "+Context.getCurrentInstance().getRequest().getServerName());
	
	    	about.setVersion(version);
	    	
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
    @ApiOperation(hidden = true, value = "")

    public Response options() {
    	
    	return Response.status(Response.Status.OK).header("Allow", "OPTIONS, GET").build();
    }
    
}

