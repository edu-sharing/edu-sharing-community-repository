package org.edu_sharing.restservices.config.v1;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.repository.server.authentication.Context;
import org.edu_sharing.restservices.ApiService;
import org.edu_sharing.restservices.RestConstants;
import org.edu_sharing.restservices.config.v1.model.Config;
import org.edu_sharing.restservices.config.v1.model.Language;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.edu_sharing.service.config.ConfigServiceFactory;
import org.edu_sharing.service.config.model.Values;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Path("/config/v1")
@Api(tags = { "CONFIG v1" })
@ApiService(value = "CONFIG", major = 1, minor = 0)
public class ConfigApi {	
	private static Logger logger = Logger.getLogger(ConfigApi.class);
	@GET
	@Path("/values")
	@ApiOperation(value = "get repository config values", notes = "Current is the actual (context-based) active config. Global is the default global config if no context is active (may be identical to the current)")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Config.class),
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })
	public Response getConfig() {
    	try {
	    	Config config=new Config();
	    	
	    	config.setGlobal(ConfigServiceFactory.getConfigService().getConfig().values);
	    	try {
	    		config.setCurrent(ConfigServiceFactory.getConfigService().getConfigByDomain(Context.getCurrentInstance().getRequest().getServerName()).values);
	    	}catch(IllegalArgumentException e) {
	    		logger.info(e.getMessage());
	    		// context for domain does not exist -> use default
	    		config.setCurrent(config.getGlobal());
	    	}
	    	return Response.status(Response.Status.OK).entity(config).build();
    	} catch (Throwable t) {
			logger.error(t.getMessage(), t);			
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse(t)).build();
		}
    }
	@GET
	@Path("/language")
	@ApiOperation(value = "get override strings for the current language", notes = "Language strings")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Language.class),
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })
	public Response getLanguage() {
    	try {
	    	Language language=new Language();
	    	language.setCurrentLanguage(new AuthenticationToolAPI().getCurrentLanguage());

	    	language.setGlobal(getActiveLanguage(ConfigServiceFactory.getConfigService().getConfig().language));
	    	try {
	    		language.setCurrent(getActiveLanguage(ConfigServiceFactory.getConfigService().getConfigByDomain(Context.getCurrentInstance().getRequest().getServerName()).language));
	    	}catch(IllegalArgumentException e) {
	    		logger.info(e.getMessage());
	    		language.setCurrent(language.getGlobal());

	    	}
	    	return Response.status(Response.Status.OK).entity(language).build();
    	} catch (Throwable t) {
			logger.error(t.getMessage(), t);			
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse(t)).build();
		}
    }
	private Map<String,String> convertStrings(List<org.edu_sharing.service.config.model.Language.String> strings) {
		Map<String,String> map=new HashMap<String, String>();
		for(org.edu_sharing.service.config.model.Language.String string : strings) {
			map.put(string.key, string.value);
		}
		return map;
	}
	private Map<String,String> getActiveLanguage(List<org.edu_sharing.service.config.model.Language> languages) {
		if(languages!=null && languages.size()>0) {
			String language=new AuthenticationToolAPI().getCurrentLanguage();
			for(org.edu_sharing.service.config.model.Language entry : languages) {
				if(entry.language.equalsIgnoreCase(language))
					return convertStrings(entry.string);
			}
			logger.info("no language override entries found in config for language "+language);
		}
		return null;
	}
}
