package org.edu_sharing.restservices.config.v1;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import io.swagger.annotations.*;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.I18nAngular;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.restservices.ApiService;
import org.edu_sharing.restservices.RestConstants;
import org.edu_sharing.restservices.config.v1.model.Config;
import org.edu_sharing.restservices.config.v1.model.Language;
import org.edu_sharing.restservices.config.v1.model.Variables;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.edu_sharing.service.config.ConfigServiceFactory;
import org.edu_sharing.service.config.DynamicConfig;

import org.json.JSONObject;

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
	    		config.setCurrent(ConfigServiceFactory.getConfigService().getConfigByDomain(ConfigServiceFactory.getCurrentDomain()).values);
	    	}catch(IllegalArgumentException e) {
	    		logger.debug(e.getMessage());
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
	@Path("/language/defaults")
	@ApiOperation(value = "get all inital language strings for angular")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Map.class),
			@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
			@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
			@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
			@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
			@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class)
	})
	public Response getLanguageDefaults() {
		try {
			return Response.status(Response.Status.OK).entity(I18nAngular.getLanguageStrings().toString()).build();
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
	    		language.setCurrent(getActiveLanguage(ConfigServiceFactory.getConfigService().getConfigByDomain(ConfigServiceFactory.getCurrentDomain()).language));
	    	}catch(IllegalArgumentException e) {
	    		logger.debug(e.getMessage());
	    		language.setCurrent(language.getGlobal());

	    	}
	    	return Response.status(Response.Status.OK).entity(language).build();
    	} catch (Throwable t) {
			logger.error(t.getMessage(), t);			
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse(t)).build();
		}
    }
	@GET
	@Path("/variables")
	@ApiOperation(value = "get global config variables", notes = "global config variables")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Variables.class),
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })
	public Response getVariables() {
    	try {
	    	Variables variables=new Variables();

	    	variables.setGlobal(convertVariables(ConfigServiceFactory.getConfigService().getConfig().variables));
	    	try {
	    		variables.setCurrent(convertVariables(ConfigServiceFactory.getConfigService().getConfigByDomain(ConfigServiceFactory.getCurrentDomain()).variables));
	    	}catch(IllegalArgumentException e) {
	    		logger.debug(e.getMessage());
	    		variables.setCurrent(variables.getGlobal());

	    	}
	    	return Response.status(Response.Status.OK).entity(variables).build();
    	} catch (Throwable t) {
			logger.error(t.getMessage(), t);			
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse(t)).build();
		}
    }
	@GET
	@Path("/dynamic/{key}")
	@ApiOperation(value = "Get a config entry (appropriate rights for the entry are required)")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = DynamicConfig.class),
			@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
			@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
			@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
			@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
			@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class)
	})
	public Response getDynamicValue(@ApiParam(value = "Key of the config value that should be fetched",required=true ) @PathParam("key") String key) {
		try {
			DynamicConfig config = ConfigServiceFactory.getConfigService().getDynamicValue(key);
			return Response.status(Response.Status.OK).entity(config).build();
		} catch (Throwable t) {
			logger.error(t.getMessage(), t);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse(t)).build();
		}
	}
	@POST
	@Path("/dynamic/{key}")
	@ApiOperation(value = "Set a config entry (admin rights required)", notes="the body must be a json encapsulated string")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = DynamicConfig.class),
			@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
			@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
			@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
			@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
			@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class)
	})
	public Response setDynamicValue(@ApiParam(value = "Key of the config value that should be fetched",required=true ) @PathParam("key") String key,
									@ApiParam(value = "Is everyone allowed to read the value",required=true ) @QueryParam("public") Boolean readPublic,
									@ApiParam(value = "Must be a json-encapsulated string",required=true )String value) {
		try {
			DynamicConfig config = ConfigServiceFactory.getConfigService().setDynamicValue(key,readPublic==null ? false : readPublic.booleanValue(), new JSONObject(value));
			return Response.status(Response.Status.OK).entity(config).build();
		} catch (Throwable t) {
			logger.error(t.getMessage(), t);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse(t)).build();
		}
	}
	private Map<String, String> convertVariables(org.edu_sharing.alfresco.service.config.model.Variables variables) {
		if(variables==null || variables.variable==null)
			return null;
		return convertKeyValue(variables.variable);
	}
	private Map<String,String> convertKeyValue(List<org.edu_sharing.alfresco.service.config.model.KeyValuePair> pairs) {
		if(pairs==null)
			return null;
		Map<String,String> map=new HashMap<String, String>();
		for(org.edu_sharing.alfresco.service.config.model.KeyValuePair pair : pairs) {
			map.put(pair.key, pair.value);
		}
		return map;
	}
	private Map<String,String> getActiveLanguage(List<org.edu_sharing.alfresco.service.config.model.Language> languages) {
		return convertKeyValue(ConfigServiceFactory.getLanguageData(languages));
	}
}
