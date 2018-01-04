package org.edu_sharing.restservices.admin.v1;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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

import org.alfresco.rest.framework.core.exceptions.InvalidArgumentException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.rpc.cache.CacheCluster;
import org.edu_sharing.repository.client.rpc.cache.CacheInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.cache.PreviewCache;
import org.edu_sharing.restservices.ApiService;
import org.edu_sharing.restservices.NodeDao;
import org.edu_sharing.restservices.RepositoryDao;
import org.edu_sharing.restservices.RestConstants;
import org.edu_sharing.restservices.admin.v1.model.AdminStatistics;
import org.edu_sharing.restservices.admin.v1.model.CollectionsResult;
import org.edu_sharing.restservices.admin.v1.model.ExcelResult;
import org.edu_sharing.restservices.admin.v1.model.UpdateResult;
import org.edu_sharing.restservices.admin.v1.model.XMLResult;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.edu_sharing.restservices.shared.Group;
import org.edu_sharing.restservices.shared.Node;
import org.edu_sharing.service.admin.AdminService;
import org.edu_sharing.service.admin.AdminServiceFactory;
import org.edu_sharing.service.admin.model.GlobalGroup;
import org.edu_sharing.service.admin.model.ServerUpdateInfo;
import org.glassfish.jersey.media.multipart.FormDataParam;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Path("/admin/v1")
@Api(tags = { "ADMIN v1" })
@ApiService(value = "ADMIN", major = 1, minor = 0)
public class AdminApi {
	public static Map<String,String[]> XML_FILTER=new HashMap<>();
	static{
		XML_FILTER.put(AdminServiceFactory.HOME_APPLICATION_PROPERTIES, new String[]{"private_key","password"});
	}
	
	private static Logger logger = Logger.getLogger(AdminApi.class);
	@POST
	@Path("/refreshAppInfo")
	
	@ApiOperation(value = "refresh app info", notes = "Refresh the application info.")
	
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Void.class),
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })
	public Response refreshAppInfo(@Context HttpServletRequest req){
		try {
			AdminServiceFactory.getInstance().refreshApplicationInfo();
	    	return Response.ok().build();		
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}
	@POST
	@Path("/applyTemplate")
	
	@ApiOperation(value = "apply a folder template", notes = "apply a folder template.")
	
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Void.class),
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })
	public Response applyTemplate(@Context HttpServletRequest req,
			@ApiParam(value = "Template Filename",required=true) @QueryParam("template") String template,
			@ApiParam(value = "Group name",required=true) @QueryParam("group") String group,
			@ApiParam(value = "Folder name",required=false) @QueryParam("folder") String folder
			){
		try {
			AdminServiceFactory.getInstance().applyTemplate(template, group, folder);
	    	return Response.ok().build();		
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}
	@OPTIONS    
	@Path("/refreshAppInfo")
    @ApiOperation(hidden = true, value = "")

	public Response options() {
		
		return Response.status(Response.Status.OK).header("Allow", "OPTIONS, POST").build();
	}
	@GET
	@Path("/applications")
	
	@ApiOperation(value = "list applications", notes = "List all registered applications.")
	
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Application[].class),
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })
	public Response getApplications(
			@Context HttpServletRequest req){
		try {
			List<Application> result = new ArrayList<Application>();
			for (ApplicationInfo appInfo : ApplicationInfoList.getRepositoryInfosOrdered()) {
				if(appInfo.ishomeNode())
					continue;
				Application entry=new Application();
				entry.setId(appInfo.getAppId());
				entry.setTitle(appInfo.getAppCaption());
				entry.setWebserverUrl(appInfo.getWebServerUrl());
				entry.setContentUrl(appInfo.getContentUrl());
				entry.setClientBaseUrl(appInfo.getClientBaseUrl());
				entry.setType(appInfo.getType());
				entry.setRepositoryType(appInfo.getRepositoryType());
				entry.setSubtype(appInfo.getSubtype());
				entry.setXml(appInfo.getXml());
				entry.setFile(appInfo.getAppFile());
				if(ApplicationInfo.TYPE_RENDERSERVICE.equals(entry.getType()) && entry.getContentUrl()!=null){
					entry.setConfigUrl(appInfo.getContentUrl().replace("/application/esmain/index.php", "/admin"));
				}
				result.add(entry);
			}
	    	return Response.ok().entity(result).build();		
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}
	@GET
	@Path("/statistics")
	
	@ApiOperation(value = "get statistics", notes = "get statistics.")
	
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = AdminStatistics.class),
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })
	public Response getStatistics(
			@Context HttpServletRequest req){
		try {
				AdminStatistics statistics=new AdminStatistics();
				AdminService service = AdminServiceFactory.getInstance();
				statistics.setActiveSessions(service.getActiveSessions());
				List<Node> locks=new ArrayList<Node>();
				for(NodeRef lock : service.getActiveNodeLocks()){
					locks.add(NodeDao.getNode(RepositoryDao.getRepository(RepositoryDao.HOME), lock.getId()).asNode());
				}
				statistics.setActiveLocks(locks);
				Runtime runtime = Runtime.getRuntime();
				statistics.setMaxMemory(runtime.maxMemory());
				statistics.setAllocatedMemory (runtime.totalMemory());
				statistics.setNumberOfPreviews (PreviewCache.getNumberOfPreviews());
				statistics.setPreviewCacheSize (PreviewCache.getTotalSize());
		    	return Response.ok().entity(statistics).build();		
			}
		catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}
	@GET
	@Path("/applications/{xml}")
	
	@ApiOperation(value = "list any xml properties (like from homeApplication.properties.xml)", notes = "list any xml properties (like from homeApplication.properties.xml)")
	
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Map.class),
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })
	
	public Response getApplicationXML(
			@Context HttpServletRequest req,
			@ApiParam(value = "Properties Filename (*.xml)",required=true) @PathParam("xml") String xml
			){
		try {
			Set<Entry<Object, Object>> set = AdminServiceFactory.getInstance().getPropertiesXML(xml).entrySet();
			Map<String,String> map=new HashMap<String, String>();
			String[] filter=XML_FILTER.get(xml);
			for(Entry<Object, Object> entry : set){
				if(filter!=null && Arrays.asList(filter).contains(entry.getKey()))
					continue;
				
				map.put((String)entry.getKey(),(String)entry.getValue());
			}
	    	return Response.ok().entity(map).build();		
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}
	@PUT
	@Path("/applications/{xml}")
	
	@ApiOperation(value = "edit any properties xml (like homeApplication.properties.xml)", notes = "if the key exists, it will be overwritten. Otherwise, it will be created. You only need to transfer keys you want to edit")
	
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Void.class),
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })
	public Response updateApplicationXML(
			@Context HttpServletRequest req,
			@ApiParam(value = "Properties Filename (*.xml)",required=true) @PathParam("xml") String xml,
			Map<String,String> properties){
		try {
			AdminServiceFactory.getInstance().updatePropertiesXML(xml,properties);
	    	return Response.ok().build();		

		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}
	@DELETE
	@Path("/applications/{id}")
	
	@ApiOperation(value = "remove an application", notes = "remove the specified application.")
	
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Void.class),
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })
	public Response removeApplication(
			@ApiParam(value = "Application id",required=true) @PathParam("id") String id,
			@Context HttpServletRequest req){
		try {
			ApplicationInfo info = ApplicationInfoList.getRepositoryInfoById(id);
			if(info==null || info.ishomeNode())
				throw new InvalidArgumentException("Application id not found or unremovable");
			AdminServiceFactory.getInstance().removeApplication(info);
	    	return Response.ok().build();		
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}
	
	@PUT
	@Path("/applications/xml")
	
	@ApiOperation(value = "register/add an application via xml file", notes = "register the xml file provided.")
	
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = HashMap.class),
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })
	public Response addApplication(
			@ApiParam(value = "XML file for app to register",required=true) @FormDataParam("xml") InputStream is,
			@Context HttpServletRequest req){
		try {
			HashMap<String, String> result = AdminServiceFactory.getInstance().addApplicationFromStream(is);
	    	return Response.ok().entity(result).build();		
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}
	
	@PUT
	@Path("/applications")
	
	@ApiOperation(value = "register/add an application", notes = "register the specified application.")
	
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = HashMap.class),
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })
	public Response addApplication(
			@ApiParam(value = "Remote application metadata url",required=true) @QueryParam("url") String url,
			@Context HttpServletRequest req){
		try {
			HashMap<String, String> result = AdminServiceFactory.getInstance().addApplication(url);
	    	return Response.ok().entity(result).build();		
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}
	
	@OPTIONS    
	@Path("/applications")
    @ApiOperation(hidden = true, value = "")

	public Response options1() {
		
		return Response.status(Response.Status.OK).header("Allow", "OPTIONS, PUT").build();
	}
	@GET
	@Path("/serverUpdate/list")
	
	@ApiOperation(value = "list available update tasks", notes = "list available update tasks")
	
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = ServerUpdateInfo[].class),
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })
	public Response serverUpdateList(
			@Context HttpServletRequest req){
		try {
			ArrayList<ServerUpdateInfo> result = AdminServiceFactory.getInstance().getServerUpdateInfos();
	    	return Response.ok().entity(result).build();		
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}
	
	@OPTIONS    
	@Path("/serverUpdate/list")
    @ApiOperation(hidden = true, value = "")

	public Response options2() {
		
		return Response.status(Response.Status.OK).header("Allow", "OPTIONS, GET").build();
	}
	@POST
	@Path("/serverUpdate/run/{id}")
	
	@ApiOperation(value = "Run an update tasks", notes = "Run a specific update task (test or full update).")
	
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = ServerUpdateInfo[].class),
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })
	public Response serverUpdateList(
			@ApiParam(value = "Id of the update task", required = true) @PathParam("id") String id,
			@ApiParam(value = "Actually execute (if false, just runs in test mode)", defaultValue="false", required = true) @QueryParam("execute") Boolean execute,
			@Context HttpServletRequest req){
		try {
			UpdateResult result = new UpdateResult(AdminServiceFactory.getInstance().runUpdate(id,execute==null ? false : execute));
	    	return Response.ok().entity(result).build();	
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}
	
	@OPTIONS    
	@Path("/serverUpdate/run/{id}")
    @ApiOperation(hidden = true, value = "")

	public Response options3() {
		
		return Response.status(Response.Status.OK).header("Allow", "OPTIONS, POST").build();
	}
	@GET
	@Path("/refreshEduGroupCache")
	
	@ApiOperation(value = "Refresh the Edu Group Cache", notes = "Refresh the Edu Group Cache.")
	
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Void.class),
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })
	public Response refreshEduGroupCache(
			@Context HttpServletRequest req){
		try {
			AdminServiceFactory.getInstance().refreshEduGroupCache();
	    	return Response.ok().build();		
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}
	
	@OPTIONS    
	@Path("/refreshEduGroupCache")
    @ApiOperation(hidden = true, value = "")

	public Response options4() {
		
		return Response.status(Response.Status.OK).header("Allow", "OPTIONS, GET").build();
	}
	@GET
	@Path("/cacheInfo/{id}")
	
	@ApiOperation(value = "Get information about a cache", notes = "Get information about a cache.")
	
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = CacheInfo.class),
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })
	public Response getCacheInfo(
			@ApiParam(value="Id/bean name of the cache") @PathParam("id") String id,
			@Context HttpServletRequest req){
		try {
			CacheInfo result = AdminServiceFactory.getInstance().getCacheInfo(id);
	    	return Response.ok().entity(result).build();		
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}
	@GET
	@Path("/catalina")
	
	@ApiOperation(value = "Get last info from catalina out", notes = "Get catalina.out log.")
	
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = String[].class),
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })
	public Response getCatalinaOut(
			@Context HttpServletRequest req){
		try {
			List<String> result = AdminServiceFactory.getInstance().getCatalinaOut();
	    	return Response.ok().entity(result).build();		
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}
	
	@OPTIONS    
	@Path("/cacheInfo/{id}")
    @ApiOperation(hidden = true, value = "")

	public Response options5() {
		
		return Response.status(Response.Status.OK).header("Allow", "OPTIONS, GET").build();
	}
	@GET
	@Path("/globalGroups")
	
	@ApiOperation(value = "Get global groups", notes = "Get global groups (groups across repositories).")
	
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Group[].class),
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })
	public Response getGlobalGroups(
			@Context HttpServletRequest req){
		try {
			List<GlobalGroup> result = AdminServiceFactory.getInstance().getGlobalGroups();
			List<Group> mapped=new ArrayList<>();
			for(GlobalGroup group : result){
				mapped.add(new Group(group));
			}
	    	return Response.ok().entity(result).build();		
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}
	
	@OPTIONS    
	@Path("/globalGroups")
    @ApiOperation(hidden = true, value = "")

	public Response options6() {
		
		return Response.status(Response.Status.OK).header("Allow", "OPTIONS, GET").build();
	}
	@POST
	@Path("/import/collections")
	
	@ApiOperation(value = "import collections via a xml file", notes = "xml file must be structured as defined by the xsd standard")
	
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = CollectionsResult.class),
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })
	public Response importCollections(
			@ApiParam(value="Id of the root to initialize the collection structure") @QueryParam("parent") String parent,
			@ApiParam(value = "XML file to parse (or zip file containing exactly 1 xml file to parse)",required=true) @FormDataParam("xml") InputStream is,
			@Context HttpServletRequest req){
		try {
			int count=AdminServiceFactory.getInstance().importCollections(parent,is);
			CollectionsResult result=new CollectionsResult();
			result.setCount(count);
	    	return Response.ok().entity(result).build();	
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}
	
	@POST
	@Path("/import/excel")
	
	@ApiOperation(value = "Import excel data", notes = "Import excel data.")
	
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = ExcelResult.class),
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })
	public Response importCSV(
			@ApiParam(value = "parent", required = true) @QueryParam("parent") String parent,
			@ApiParam(value = "Excel file to import",required=true) @FormDataParam("excel") InputStream is,
			@Context HttpServletRequest req){
		try {
			int count=AdminServiceFactory.getInstance().importExcel(parent,is);
			ExcelResult result = new ExcelResult();
			result.setRows(count);
	    	return Response.ok().entity(result).build();	
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}
	
	@POST
	@Path("/import/oai")
	
	@ApiOperation(value = "Import oai data", notes = "Import oai data.")
	
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Void.class),
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })
	public Response importOai(
			@ApiParam(value = "base url", required = true) @QueryParam("baseUrl") String baseUrl,
			@ApiParam(value = "set/catalog id", required = true) @QueryParam("set") String set,
			@ApiParam(value = "metadata prefix", required = true) @QueryParam("metadataPrefix") String metadataPrefix,
			@ApiParam(value = "id metadataset", required = false) @QueryParam("metadataset") String metadataset,
			@ApiParam(value = "importer job class name (call /classes to obtain a list)", required = true, defaultValue="org.edu_sharing.repository.server.jobs.quartz.ImporterJob") @QueryParam("className") String className,
			@ApiParam(value = "importer class name (call /classes to obtain a list)", required = false, defaultValue="org.edu_sharing.repository.server.importer.OAIPMHLOMImporter") @QueryParam("importerClassName") String importerClassName,
			@ApiParam(value = "RecordHandler class name", required = false, defaultValue="org.edu_sharing.repository.server.importer.RecordHandlerLOM") @QueryParam("recordHandlerClassName") String recordHandlerClassName,
			@ApiParam(value = "BinaryHandler class name (may be empty for none)", required = false, defaultValue="") @QueryParam("binaryHandlerClassName") String binaryHandlerClassName,
			@ApiParam(value = "url to file", required = false) @QueryParam("fileUrl") String fileUrl,
			@Context HttpServletRequest req){
		try {
			AdminServiceFactory.getInstance().importOai(
					set, fileUrl, baseUrl, metadataset, 
					metadataPrefix, className, 
					importerClassName, recordHandlerClassName,
					binaryHandlerClassName);
	    	return Response.ok().build();	
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}
	
	@OPTIONS    
	@Path("/import/oai")
    @ApiOperation(hidden = true, value = "")

	public Response options7() {
		
		return Response.status(Response.Status.OK).header("Allow", "OPTIONS, POST, DELETE").build();
	}
	@GET
	@Path("/import/oai/classes")
	
	@ApiOperation(value = "Get OAI class names", notes = "Get available importer classes for OAI import.")
	
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = String[].class),
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })
	public Response getOaiClasses(
		@Context HttpServletRequest req){
		try {
			List<String> result = AdminServiceFactory.getInstance().getImporterClasses();
	    	return Response.ok().entity(result).build();	
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}
	
	@OPTIONS    
	@Path("/import/oai/classes")
    @ApiOperation(hidden = true, value = "")

	public Response options8() {
		
		return Response.status(Response.Status.OK).header("Allow", "OPTIONS, GET").build();
	}
	@POST
	@Path("/import/refreshCache/{folder}")
	
	@ApiOperation(value = "Refresh cache", notes = "Refresh importer cache.")
	
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Void.class),
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })
	public Response refreshCache(
		@ApiParam(value = "refresh cache root folder id", required = true) @PathParam("rootFolder") String rootFolder,
		@ApiParam(value = "sticky", required = true,defaultValue="false") @QueryParam("sticky") Boolean sticky,
		@Context HttpServletRequest req){
		try {
			AdminServiceFactory.getInstance().startCacheRefreshingJob(rootFolder, sticky==null ? false : true);
	    	return Response.ok().build();	
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}
	
	@OPTIONS    
	@Path("/import/refreshCache/{folder}")
    @ApiOperation(hidden = true, value = "")

	public Response options9() {
		
		return Response.status(Response.Status.OK).header("Allow", "OPTIONS, POST").build();
	}
	@DELETE
	@Path("/import/oai")
	
	@ApiOperation(value = "Remove deleted imports", notes = "Remove deleted imports.")
	
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Void.class),
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })
	public Response removeOaiImports(
			@ApiParam(value = "base url", required = true) @QueryParam("baseUrl") String baseUrl,
			@ApiParam(value = "set/catalog id", required = true) @QueryParam("set") String set,
			@ApiParam(value = "metadata prefix", required = true) @QueryParam("metadataPrefix") String metadataPrefix,
			@Context HttpServletRequest req){
		try {
			AdminServiceFactory.getInstance().removeDeletedImports(baseUrl, set, metadataPrefix);
	    	return Response.ok().build();	
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}
	
	@GET
	@Path("/propertyToMds")
	
	@ApiOperation(value = "Get a Mds Valuespace for all values of the given properties", notes = "Get a Mds Valuespace for all values of the given properties.")
	
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = String.class),
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })
	public Response getPropertyToMds(
		@ApiParam(value = "one or more properties", required = true) @QueryParam("properties") List<String> properties,
		@Context HttpServletRequest req){
		try {
			XMLResult result = new XMLResult();
			result.setXml(AdminServiceFactory.getInstance().getPropertyToMDSXml(properties));
	    	return Response.ok().entity(result).build();	
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}
	
	@OPTIONS    
	@Path("/propertyToMds")
    @ApiOperation(hidden = true, value = "")

	public Response options10() {
		
		return Response.status(Response.Status.OK).header("Allow", "OPTIONS, GET").build();
	}
	
	
	@GET
	@Path("/clusterInfo")
	@ApiOperation(value = "Get information about the Cluster", notes = "Get information the Cluster")
	
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = CacheCluster.class),
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })
	public Response getCluster(
			@Context HttpServletRequest req){
		try {
			CacheCluster result = AdminServiceFactory.getInstance().getCacheCluster();
	    	return Response.ok().entity(result).build();		
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}
	
	@OPTIONS    
	@Path("/clusterInfo")
    @ApiOperation(hidden = true, value = "")
	

	public Response options11() {
		
		return Response.status(Response.Status.OK).header("Allow", "OPTIONS, GET").build();
	}
	
	@GET
	@Path("/clusterInfos")
	@ApiOperation(value = "Get information about the Cluster", notes = "Get information the Cluster")
	
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = CacheCluster.class),
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })
	public Response getClusters(
			@Context HttpServletRequest req){
		try {
			List<CacheCluster> result = AdminServiceFactory.getInstance().getCacheClusters();
	    	return Response.ok().entity(result).build();		
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}
	
	@OPTIONS    
	@Path("/clusterInfos")
    @ApiOperation(hidden = true, value = "")
	

	public Response options12() {
		
		return Response.status(Response.Status.OK).header("Allow", "OPTIONS, GET").build();
	}
	
	
	@GET
	@Path("/export/lom")
	
	@ApiOperation(value = "Export Nodes with LOM Metadata Format", notes = "Export Nodes with LOM Metadata Format.")
	
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Void.class),
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })
	public Response exportLOM(
		@ApiParam(value = "filterQuery", required = true) @QueryParam("filterQuery") String filterQuery,
		@ApiParam(value = "targetDir", required = true) @QueryParam("targetDir") String targetDir,
		@ApiParam(value = "subObjectHandler", required = true) @QueryParam("subObjectHandler") Boolean subObjectHandler,
		@Context HttpServletRequest req){
		try {
			AdminServiceFactory.getInstance().exportLom(filterQuery, targetDir, subObjectHandler);
			return Response.ok().build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}
	
	@OPTIONS    
	@Path("/export/lom")
    @ApiOperation(hidden = true, value = "")

	public Response options13() {
		
		return Response.status(Response.Status.OK).header("Allow", "OPTIONS, GET").build();
	}

}
