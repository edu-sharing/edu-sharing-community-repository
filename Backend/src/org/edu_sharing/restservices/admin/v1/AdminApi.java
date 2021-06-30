package org.edu_sharing.restservices.admin.v1;

import java.io.FileWriter;
import java.io.InputStream;
import java.io.Serializable;
import java.io.Writer;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.alfresco.rest.framework.core.exceptions.InvalidArgumentException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.rpc.cache.CacheCluster;
import org.edu_sharing.repository.client.rpc.cache.CacheInfo;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.repository.server.jobs.quartz.JobDescription;
import org.edu_sharing.repository.server.jobs.quartz.JobInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.cache.PreviewCache;
import org.edu_sharing.restservices.ApiService;
import org.edu_sharing.restservices.CollectionDao;
import org.edu_sharing.restservices.DAOException;
import org.edu_sharing.restservices.NodeDao;
import org.edu_sharing.restservices.RepositoryDao;
import org.edu_sharing.restservices.RestConstants;
import org.edu_sharing.restservices.admin.v1.model.AdminStatistics;
import org.edu_sharing.restservices.admin.v1.model.CollectionsResult;
import org.edu_sharing.restservices.admin.v1.model.ExcelResult;
import org.edu_sharing.restservices.admin.v1.model.UpdateResult;
import org.edu_sharing.restservices.admin.v1.model.UploadResult;
import org.edu_sharing.restservices.admin.v1.model.XMLResult;
import org.edu_sharing.restservices.shared.*;
import org.edu_sharing.service.NotAnAdminException;
import org.edu_sharing.service.admin.AdminService;
import org.edu_sharing.service.admin.AdminServiceFactory;
import org.edu_sharing.service.admin.model.RepositoryConfig;
import org.edu_sharing.service.admin.model.GlobalGroup;
import org.edu_sharing.service.admin.model.ServerUpdateInfo;
import org.edu_sharing.service.lifecycle.PersonDeleteOptions;
import org.edu_sharing.service.lifecycle.PersonLifecycleService;
import org.edu_sharing.service.lifecycle.PersonReport;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.search.SearchResultNodeRefElastic;
import org.edu_sharing.service.search.SearchService.ContentType;
import org.edu_sharing.service.search.SearchServiceElastic;
import org.edu_sharing.service.search.model.SearchToken;
import org.edu_sharing.service.search.model.SortDefinition;
import org.edu_sharing.service.admin.model.ToolPermission;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
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
	static Map<String, String[]> XML_FILTER = new HashMap<>();
	static {
		XML_FILTER.put(AdminServiceFactory.HOME_APPLICATION_PROPERTIES, new String[] { "private_key", "password" });
	}

	private static Logger logger = Logger.getLogger(AdminApi.class);

	@POST
	@Path("/refreshAppInfo")

	@ApiOperation(value = "refresh app info", notes = "Refresh the application info.")

	@ApiResponses(value = { @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Void.class),
			@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
			@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
			@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
			@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
			@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) })
	public Response refreshAppInfo(@Context HttpServletRequest req) {
		try {
			AdminServiceFactory.getInstance().refreshApplicationInfo();
			return Response.ok().build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}
	@GET
	@Path("/toolpermissions/{authority}")

	@ApiOperation(value = "get all toolpermissions for an authority", notes="Returns explicit (rights set for this authority) + effective (resulting rights for this authority) toolpermission")

	@ApiResponses(value = {
			@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Map.class),
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class)
	    })
	public Response getAllToolpermissions(
			@ApiParam(value = "Authority to load (user or group)",required=true) @PathParam("authority") String authority,
			@Context HttpServletRequest req){
		try {
			Map<String, ToolPermission> result = AdminServiceFactory.getInstance().getToolpermissions(authority);
	    	return Response.ok().entity(result).build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}
	@PUT
	@Path("/toolpermissions/{authority}")

	@ApiOperation(value = "set toolpermissions for an authority", notes="If a toolpermission has status UNDEFINED, it will remove explicit permissions for the authority")

	@ApiResponses(value = {
			@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Map.class),
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class)
	    })
	public Response setToolpermissions(
			@ApiParam(value = "Authority to set (user or group)",required=true) @PathParam("authority") String authority,
			Map<String,ToolPermission.Status> permissions,
			@Context HttpServletRequest req){
		try {
			AdminServiceFactory.getInstance().setToolpermissions(authority,permissions);
	    	return Response.ok().build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}
	@POST
	@Path("/applyTemplate")

	@ApiOperation(value = "apply a folder template", notes = "apply a folder template.")

	@ApiResponses(value = { @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Void.class),
			@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
			@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
			@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
			@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
			@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) })
	public Response applyTemplate(@Context HttpServletRequest req,
			@ApiParam(value = "Template Filename", required = true) @QueryParam("template") String template,
			@ApiParam(value = "Group name (authority name)", required = true) @QueryParam("group") String group,
			@ApiParam(value = "Folder name", required = false) @QueryParam("folder") String folder) {
		try {
			AdminServiceFactory.getInstance().applyTemplate(template, group, folder);
			return Response.ok().build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}

	@POST
	@Path("/toolpermissions/add/{name}")

	@ApiOperation(value = "add a new toolpermissions")

	@ApiResponses(value = {
			@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Node.class),
			@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
			@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
			@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
			@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
			@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class)
	})
	public Response addToolpermission(
			@ApiParam(value = "Name/ID of toolpermission",required=true) @PathParam("name") String name,
			@Context HttpServletRequest req){
		try {
			String nodeId=AdminServiceFactory.getInstance().addToolpermission(name);
			NodeDao nodeDao=NodeDao.getNode(RepositoryDao.getHomeRepository(),nodeId);
			return Response.ok().entity(nodeDao.asNode()).build();
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

	@ApiResponses(value = { @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Application[].class),
			@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
			@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
			@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
			@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
			@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) })
	public Response getApplications(@Context HttpServletRequest req) {
		try {
			List<Application> result = new ArrayList<Application>();
			for (ApplicationInfo appInfo : ApplicationInfoList.getRepositoryInfosOrdered()) {
				if (appInfo.ishomeNode())
					continue;
				Application entry = new Application();
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
				if (ApplicationInfo.TYPE_RENDERSERVICE.equals(entry.getType()) && entry.getContentUrl() != null) {
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
	@Path("/jobs")

	@ApiOperation(value = "get all running jobs")

	@ApiResponses(value = { @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = JobInfo[].class),
			@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
			@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
			@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
			@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
			@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) })
	public Response getJobs(@Context HttpServletRequest req) {
		try {
			return Response.ok().entity(AdminServiceFactory.getInstance().getJobs()).build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}

	@GET
	@Path("/jobs/all")

	@ApiOperation(value = "get all available jobs")

	@ApiResponses(value = { @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = JobDescription[].class),
			@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
			@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
			@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
			@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
			@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) })
	public Response getAllJobs(@Context HttpServletRequest req) {
		try {
			return Response.ok().entity(AdminServiceFactory.getInstance().getJobDescriptions()).build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}



	@DELETE
	@Path("/jobs/{job}")

	@ApiOperation(value = "cancel a running job")

	@ApiResponses(value = { @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Void.class),
			@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
			@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
			@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
			@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
			@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) })
	public Response cancelJob(@Context HttpServletRequest req,
							  @PathParam("job") String name) {
		try {
			AdminServiceFactory.getInstance().cancelJob(name);
			return Response.ok().build();
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
			@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) })
	public Response getStatistics(@Context HttpServletRequest req) {
		try {
			AdminStatistics statistics = new AdminStatistics();
			AdminService service = AdminServiceFactory.getInstance();
			statistics.setActiveSessions(service.getActiveSessions());
			List<Node> locks = new ArrayList<Node>();
			for (NodeRef lock : service.getActiveNodeLocks()) {
				locks.add(NodeDao.getNode(RepositoryDao.getRepository(RepositoryDao.HOME), lock.getId()).asNode());
			}
			statistics.setActiveLocks(locks);
			Runtime runtime = Runtime.getRuntime();
			statistics.setMaxMemory(runtime.maxMemory());
			statistics.setAllocatedMemory(runtime.totalMemory());
			statistics.setNumberOfPreviews(PreviewCache.getNumberOfPreviews());
			statistics.setPreviewCacheSize(PreviewCache.getTotalSize());
			return Response.ok().entity(statistics).build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}

	@GET
	@Path("/applications/{xml}")

	@ApiOperation(value = "list any xml properties (like from homeApplication.properties.xml)", notes = "list any xml properties (like from homeApplication.properties.xml)")

	@ApiResponses(value = { @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Map.class),
			@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
			@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
			@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
			@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
			@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) })

	public Response getApplicationXML(@Context HttpServletRequest req,
			@ApiParam(value = "Properties Filename (*.xml)", required = true) @PathParam("xml") String xml) {
		try {
			Set<Entry<Object, Object>> set = AdminServiceFactory.getInstance().getPropertiesXML(xml).entrySet();
			Map<String, String> map = new HashMap<String, String>();
			String[] filter = XML_FILTER.get(xml);
			for (Entry<Object, Object> entry : set) {
				if (filter != null && Arrays.asList(filter).contains(entry.getKey()))
					continue;

				map.put((String) entry.getKey(), (String) entry.getValue());
			}
			return Response.ok().entity(map).build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}

	@PUT
	@Path("/applications/{xml}")

	@ApiOperation(value = "edit any properties xml (like homeApplication.properties.xml)", notes = "if the key exists, it will be overwritten. Otherwise, it will be created. You only need to transfer keys you want to edit")

	@ApiResponses(value = { @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Void.class),
			@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
			@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
			@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
			@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
			@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) })
	public Response updateApplicationXML(@Context HttpServletRequest req,
			@ApiParam(value = "Properties Filename (*.xml)", required = true) @PathParam("xml") String xml,
			Map<String, String> properties) {
		try {
			AdminServiceFactory.getInstance().updatePropertiesXML(xml, properties);
			return Response.ok().build();

		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}

	@DELETE
	@Path("/applications/{id}")

	@ApiOperation(value = "remove an application", notes = "remove the specified application.")

	@ApiResponses(value = { @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Void.class),
			@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
			@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
			@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
			@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
			@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) })
	public Response removeApplication(@ApiParam(value = "Application id", required = true) @PathParam("id") String id,
			@Context HttpServletRequest req) {
		try {
			ApplicationInfo info = ApplicationInfoList.getRepositoryInfoById(id);
			if (info == null || info.ishomeNode())
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

	@ApiResponses(value = { @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = HashMap.class),
			@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
			@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
			@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
			@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
			@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) })
	public Response addApplication(
			@ApiParam(value = "XML file for app to register", required = true) @FormDataParam("xml") InputStream is,
			@Context HttpServletRequest req) {
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

	@ApiResponses(value = { @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = HashMap.class),
			@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
			@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
			@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
			@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
			@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) })
	public Response addApplication(
			@ApiParam(value = "Remote application metadata url", required = true) @QueryParam("url") String url,
			@Context HttpServletRequest req) {
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
			@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) })
	public Response serverUpdateList(@Context HttpServletRequest req) {
		try {
			List<ServerUpdateInfo> result = AdminServiceFactory.getInstance().getServerUpdateInfos();
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
			@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) })
	public Response serverUpdateList(
			@ApiParam(value = "Id of the update task", required = true) @PathParam("id") String id,
			@ApiParam(value = "Actually execute (if false, just runs in test mode)", defaultValue = "false", required = true) @QueryParam("execute") Boolean execute,
			@Context HttpServletRequest req) {
		try {
			UpdateResult result = new UpdateResult(
					AdminServiceFactory.getInstance().runUpdate(id, execute == null ? false : execute));
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

	@POST
	@Path("/cache/refreshEduGroupCache")

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
            @ApiParam(value="keep existing", defaultValue="false") @QueryParam("keepExisting") Boolean keepExisting,
            @Context HttpServletRequest req){
		try {
            AdminServiceFactory.getInstance().refreshEduGroupCache(keepExisting);
	    	return Response.ok().build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}

	@OPTIONS
	@Path("/cache/refreshEduGroupCache")
	@ApiOperation(hidden = true, value = "")

	public Response options4() {

		return Response.status(Response.Status.OK).header("Allow", "OPTIONS, GET").build();
	}

	@POST
	@Path("/cache/removeCacheEntry")

	@ApiOperation(value = "remove cache entry", notes = "remove cache entry")

	@ApiResponses(value = {
			@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Void.class),
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class)
	    })
	public Response removeCacheEntry(
            @ApiParam(value="cacheIndex") @QueryParam("cacheIndex") Integer cacheIndex,
            @ApiParam(value="bean") @QueryParam("bean") String bean,
            @Context HttpServletRequest req){
		try {
            AdminServiceFactory.getInstance().removeCacheEntry(cacheIndex, bean);
	    	return Response.ok().build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}

	@POST
	@Path("/cache/clearCache")

	@ApiOperation(value = "clear cache", notes = "clear cache")

	@ApiResponses(value = {
			@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Void.class),
			@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
			@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
			@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
			@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
			@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class)
	})
	public Response clearCache(

			@ApiParam(value="bean") @QueryParam("bean") String bean,
			@Context HttpServletRequest req){
		try {
			AdminServiceFactory.getInstance().clearCache(bean);
			return Response.ok().build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}

	@GET
	@Path("/cache/cacheEntries/{id}")

	@ApiOperation(value = "Get entries of a cache", notes = "Get entries of a cache.")

	@ApiResponses(value = { @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Map.class),
			@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
			@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
			@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
			@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
			@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) })
	public Response getCacheEntries(@ApiParam(value = "Id/bean name of the cache") @PathParam("id") String id,
								 @Context HttpServletRequest req) {
		try {
			Map<Serializable, Serializable> result = AdminServiceFactory.getInstance().getCacheEntries(id);
			return Response.ok().entity(result).build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}



	@GET
	@Path("/cache/cacheInfo/{id}")

	@ApiOperation(value = "Get information about a cache", notes = "Get information about a cache.")

	@ApiResponses(value = { @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = CacheInfo.class),
			@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
			@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
			@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
			@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
			@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) })
	public Response getCacheInfo(@ApiParam(value = "Id/bean name of the cache") @PathParam("id") String id,
			@Context HttpServletRequest req) {
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

	@ApiResponses(value = { @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = String[].class),
			@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
			@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
			@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
			@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
			@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) })
	public Response getCatalinaOut(@Context HttpServletRequest req) {
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

	@ApiResponses(value = { @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Group[].class),
			@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
			@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
			@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
			@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
			@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) })
	public Response getGlobalGroups(@Context HttpServletRequest req) {
		try {
			List<GlobalGroup> result = AdminServiceFactory.getInstance().getGlobalGroups();
			List<Group> mapped = new ArrayList<>();
			for (GlobalGroup group : result) {
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
			@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) })
	public Response importCollections(
			@ApiParam(value = "Id of the root to initialize the collection structure, or '-root-' to inflate them on the first level") @QueryParam("parent") String parent,
			@ApiParam(value = "XML file to parse (or zip file containing exactly 1 xml file to parse)", required = true) @FormDataParam("xml") InputStream is,
			@Context HttpServletRequest req) {
		try {
			if (CollectionDao.ROOT.equals(parent)) {
				parent = null;
			}
			int count = AdminServiceFactory.getInstance().importCollections(parent, is);
			CollectionsResult result = new CollectionsResult();
			result.setCount(count);
			return Response.ok().entity(result).build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}

	@POST
	@Path("/import/excel")

	@ApiOperation(value = "Import excel data", notes = "Import excel data.")

	@ApiResponses(value = { @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = ExcelResult.class),
			@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
			@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
			@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
			@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
			@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) })
	public Response importExcel(@ApiParam(value = "parent", required = true) @QueryParam("parent") String parent,
								@ApiParam(value = "addToCollection", required = true, defaultValue = "false") @QueryParam("addToCollection") Boolean addToCollection,
			@ApiParam(value = "Excel file to import", required = true) @FormDataParam("excel") InputStream is,
			@Context HttpServletRequest req) {
		try {
			addToCollection = (addToCollection == null) ? false : addToCollection;
			int count = AdminServiceFactory.getInstance().importExcel(parent, is, addToCollection);
			ExcelResult result = new ExcelResult();
			result.setRows(count);
			return Response.ok().entity(result).build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}

	@PUT
	@Path("/upload/temp/{name}")

	@ApiOperation(value = "Upload a file", notes = "Upload a file to tomcat temp directory, to use it on the server (e.g. an update)")

	@ApiResponses(value = { @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = UploadResult.class),
			@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
			@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
			@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
			@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
			@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) })
	public Response uploadTemp(@ApiParam(value = "filename", required = true) @PathParam("name") String name,
			@ApiParam(value = "file to upload", required = true) @FormDataParam("file") InputStream is,
			@Context HttpServletRequest req) {
		try {
			String file = AdminServiceFactory.getInstance().uploadTemp(name, is);
			return Response.ok().entity(new UploadResult(file)).build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}

	@POST
	@Path("/import/oai")

	@ApiOperation(value = "Import oai data", notes = "Import oai data.")

	@ApiResponses(value = { @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Void.class),
			@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
			@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
			@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
			@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
			@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) })
	public Response importOai(@ApiParam(value = "base url", required = true) @QueryParam("baseUrl") String baseUrl,
			@ApiParam(value = "set/catalog id", required = true) @QueryParam("set") String set,
			@ApiParam(value = "metadata prefix", required = true) @QueryParam("metadataPrefix") String metadataPrefix,
			@ApiParam(value = "id metadataset", required = false) @QueryParam("metadataset") String metadataset,
			@ApiParam(value = "importer job class name (call /classes to obtain a list)", required = true, defaultValue = "org.edu_sharing.repository.server.jobs.quartz.ImporterJob") @QueryParam("className") String className,
			@ApiParam(value = "importer class name (call /classes to obtain a list)", required = false, defaultValue = "org.edu_sharing.repository.server.importer.OAIPMHLOMImporter") @QueryParam("importerClassName") String importerClassName,
			@ApiParam(value = "RecordHandler class name", required = false, defaultValue = "org.edu_sharing.repository.server.importer.RecordHandlerLOM") @QueryParam("recordHandlerClassName") String recordHandlerClassName,
			@ApiParam(value = "BinaryHandler class name (may be empty for none)", required = false, defaultValue = "") @QueryParam("binaryHandlerClassName") String binaryHandlerClassName,
		    @ApiParam(value = "PersistentHandlerClassName class name (may be empty for none)", required = false, defaultValue = "") @QueryParam("persistentHandlerClassName") String persistentHandlerClassName,
			@ApiParam(value = "url to file", required = false) @QueryParam("fileUrl") String fileUrl,
			@ApiParam(value = "OAI Ids to import, can be null than the whole set will be imported", required = false, defaultValue = "") @QueryParam("oaiIds") String oaiIds,
			@ApiParam(value = "force Update of all entries", required = false, defaultValue = "false") @QueryParam("forceUpdate") Boolean forceUpdate,
			@ApiParam(value = "from: datestring yyyy-MM-dd)", required = false, defaultValue = "") @QueryParam("from") String from,
			@ApiParam(value = "until: datestring yyyy-MM-dd)", required = false, defaultValue = "") @QueryParam("until") String until,
		  	@ApiParam(value = "periodInDays: internal sets from and until. only effective if from/until not set)", required = false, defaultValue = "") @QueryParam("periodInDays") String periodInDays,
		    @Context HttpServletRequest req) {
		try {
			AdminServiceFactory.getInstance().importOai(set, fileUrl, baseUrl, metadataset, metadataPrefix, className,
					importerClassName, recordHandlerClassName, binaryHandlerClassName,persistentHandlerClassName, oaiIds, forceUpdate != null ? forceUpdate.booleanValue() : false, from, until, periodInDays);
			return Response.ok().build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}

	@POST
	@Path("/import/oai/xml")
	@ApiOperation(value = "Import single xml via oai (for testing)")
	@ApiResponses(value = { @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Node.class),
			@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
			@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
			@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
			@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
			@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) })
	public Response importOaiXML (@ApiParam(value = "RecordHandler class name", required = false, defaultValue = "org.edu_sharing.repository.server.importer.RecordHandlerLOM") @QueryParam("recordHandlerClassName") String recordHandlerClassName,
							  @ApiParam(value = "BinaryHandler class name (may be empty for none)", required = false, defaultValue = "") @QueryParam("binaryHandlerClassName") String binaryHandlerClassName,
							  @FormDataParam("xml") InputStream xml,
								@Context HttpServletRequest req) {
		try {
			String node=AdminServiceFactory.getInstance().importOaiXml(xml, recordHandlerClassName, binaryHandlerClassName);
			if(node==null) {
				throw new Exception("Importer failed to create node");
			}
			Node result=NodeDao.getNode(RepositoryDao.getHomeRepository(),node,Filter.createShowAllFilter()).asNode();
			return Response.ok().entity(result).build();
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

	@ApiResponses(value = { @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = String[].class),
			@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
			@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
			@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
			@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
			@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) })
	public Response getOaiClasses(@Context HttpServletRequest req) {
		try {
			List<String> result =
					AdminServiceFactory.getInstance().getImporterClasses().stream().
							map(Class::getName).collect(Collectors.toList());
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

	@ApiResponses(value = { @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Void.class),
			@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
			@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
			@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
			@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
			@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) })
	public Response refreshCache(
			@ApiParam(value = "refresh cache root folder id", defaultValue = "-userhome-", required = true) @PathParam("folder") String rootFolder,
			@ApiParam(value = "sticky", required = true, defaultValue = "false") @QueryParam("sticky") Boolean sticky,
			@Context HttpServletRequest req) {
		try {
			AdminServiceFactory.getInstance().startCacheRefreshingJob(NodeDao.mapNodeConstants(RepositoryDao.getHomeRepository(),rootFolder), sticky != null && sticky);
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

	@ApiResponses(value = { @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Void.class),
			@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
			@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
			@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
			@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
			@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) })
	public Response removeOaiImports(
			@ApiParam(value = "base url", required = true) @QueryParam("baseUrl") String baseUrl,
			@ApiParam(value = "set/catalog id", required = true) @QueryParam("set") String set,
			@ApiParam(value = "metadata prefix", required = true) @QueryParam("metadataPrefix") String metadataPrefix,
			@Context HttpServletRequest req) {
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

	@ApiResponses(value = { @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = String.class),
			@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
			@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
			@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
			@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
			@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) })
	public Response getPropertyToMds(
			@ApiParam(value = "one or more properties", required = true) @QueryParam("properties") List<String> properties,
			@Context HttpServletRequest req) {
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

	@ApiResponses(value = { @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = CacheCluster.class),
			@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
			@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
			@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
			@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
			@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) })
	public Response getCluster(@Context HttpServletRequest req) {
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

	@ApiResponses(value = { @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = CacheCluster.class),
			@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
			@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
			@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
			@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
			@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) })
	public Response getClusters(@Context HttpServletRequest req) {
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

	@POST
	@Path("/mail/{receiver}/{template}")
	@ApiOperation(value = "Test a mail template", notes = "Sends the given template as a test to the given receiver.")
	@ApiResponses(value = { @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Void.class),
			@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
			@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
			@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
			@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
			@ApiResponse(code = 409, message = RestConstants.HTTP_409, response = ErrorResponse.class),
			@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) })

	public Response testMail(
			@ApiParam(required = true) @PathParam("receiver") String receiver,
			@ApiParam(required = true) @PathParam("template") String template,
			@Context HttpServletRequest req) {
		try {
			AdminServiceFactory.getInstance().testMail(receiver,template);
			return Response.ok().build();
		} catch (NotAnAdminException e) {
			return ErrorResponse.createResponse(e);
		} catch (Exception e) {
			return ErrorResponse.createResponse(e);
		}

	}

	@GET
	@Path("/export/lom")

	@ApiOperation(value = "Export Nodes with LOM Metadata Format", notes = "Export Nodes with LOM Metadata Format.")

	@ApiResponses(value = { @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Void.class),
			@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
			@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
			@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
			@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
			@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) })
	public Response exportLOM(
			@ApiParam(value = "filterQuery", required = true) @QueryParam("filterQuery") String filterQuery,
			@ApiParam(value = "targetDir", required = true) @QueryParam("targetDir") String targetDir,
			@ApiParam(value = "subObjectHandler", required = true) @QueryParam("subObjectHandler") Boolean subObjectHandler,
			@Context HttpServletRequest req) {
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

	@POST
	@Path("/job/{jobClass}")
	@ApiOperation(value = "Start a Job.", notes = "Start a Job.")
	@ApiResponses(value = { @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Void.class),
			@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
			@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
			@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
			@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
			@ApiResponse(code = 409, message = RestConstants.HTTP_409, response = ErrorResponse.class),
			@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) })

	public Response startJob(
			@ApiParam(value = "jobClass", required = true) @PathParam("jobClass") String jobClass,
			@ApiParam(value = "params", required = true) HashMap<String, String> params,
			@Context HttpServletRequest req) {
		try {
			AdminServiceFactory.getInstance().startJob(jobClass, new HashMap<String,Object>(params));
			return Response.ok().build();
		} catch (NotAnAdminException e) {
			return ErrorResponse.createResponse(e);
		} catch (Exception e) {
			return ErrorResponse.createResponse(e);
		}

	}

	@OPTIONS
	@Path("/job")
	@ApiOperation(hidden = true, value = "")

	public Response options14() {

		return Response.status(Response.Status.OK).header("Allow", "OPTIONS, POST").build();
	}

	@GET
	@Path("/elastic")
	@Consumes({ "application/json" })

	@ApiOperation(value = "Search for custom elastic DSL query")

	@ApiResponses(value = { @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = SearchResultElastic.class),
			@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
			@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
			@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
			@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
			@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) })

	public Response searchByElasticDSL(
			@ApiParam(value = "dsl query (json encoded)", defaultValue = "") @QueryParam("dsl") String dsl,
			@Context HttpServletRequest req) {

		try {

			//check that there is an admin
			AdminServiceFactory.getInstance();
			SearchServiceElastic elastic = new SearchServiceElastic(ApplicationInfoList.getHomeRepository().getAppId());
			SearchResultNodeRefElastic search = elastic.searchDSL(dsl);
			RepositoryDao repoDao = RepositoryDao.getHomeRepository();
			List<Node> data = new ArrayList<Node>();
			for (org.edu_sharing.service.model.NodeRef ref : search.getData()) {
				try {
					data.add(NodeDao.getNode(repoDao, ref).asNode());
				}catch(Throwable t){
					logger.warn("Error mapping elastic node " + ref.getNodeId(), t);
				}
			}

			Pagination pagination = new Pagination();
			pagination.setFrom(search.getStartIDX());
			pagination.setCount(data.size());
			pagination.setTotal(search.getNodeCount());

			SearchResultElastic<Node> response = new SearchResultElastic<>();

			response.setNodes(data);
			response.setPagination(pagination);

			XContentBuilder builder = XContentFactory.jsonBuilder().prettyPrint();
			search.getElasticResponse().toXContent(builder, ToXContent.EMPTY_PARAMS);
			response.setElasticResponse(Strings.toString(builder));
			return Response.status(Response.Status.OK).entity(response).build();

		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}
	@GET
	@Path("/lucene")
	@Consumes({ "application/json" })

	@ApiOperation(value = "Search for custom lucene query", notes = "e.g. @cm\\:name:\"*\"")

	@ApiResponses(value = { @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = SearchResult.class),
			@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
			@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
			@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
			@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
			@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) })

	public Response searchByLucene(
			@ApiParam(value = "query", defaultValue = "@cm\\:name:\"*\"") @QueryParam("query") String query,
			@ApiParam(value = "maximum items per page", defaultValue = "10") @QueryParam("maxItems") Integer maxItems,
			@ApiParam(value = "skip a number of items", defaultValue = "0") @QueryParam("skipCount") Integer skipCount,
			@ApiParam(value = RestConstants.MESSAGE_SORT_PROPERTIES) @QueryParam("sortProperties") List<String> sortProperties,
			@ApiParam(value = RestConstants.MESSAGE_SORT_ASCENDING) @QueryParam("sortAscending") List<Boolean> sortAscending,
			@ApiParam(value = "property filter for result nodes (or \"-all-\" for all properties)", defaultValue = "-all-") @QueryParam("propertyFilter") List<String> propertyFilter,
			@ApiParam(value = "store, workspace or archive") @QueryParam("store") LuceneStore store,
			@ApiParam(value = "authority scope to search for") @QueryParam("authorityScope") List<String> authorityScope,
			@Context HttpServletRequest req) {

		try {

			//check that there is an admin
			AdminServiceFactory.getInstance();

			Filter filter = new Filter(propertyFilter);
			RepositoryDao repoDao = RepositoryDao.getHomeRepository();

			SearchToken token = new SearchToken();
			token.setSortDefinition(new SortDefinition(sortProperties, sortAscending));
			token.setFrom(skipCount != null ? skipCount : 0);
			token.setMaxResult(maxItems != null ? maxItems : RestConstants.DEFAULT_MAX_ITEMS);
			token.setContentType(ContentType.ALL);
			token.setLuceneString(query);
			StoreRef storeRef = LuceneStore.Archive.equals(store) ? StoreRef.STORE_REF_ARCHIVE_SPACESSTORE : StoreRef.STORE_REF_WORKSPACE_SPACESSTORE;
			if (LuceneStore.Archive.equals(store)) {
				token.setStoreName(storeRef.getIdentifier());
				token.setStoreProtocol(storeRef.getProtocol());
			}
			token.disableSearchCriterias();
			token.setAuthorityScope(authorityScope);
			NodeSearch search = NodeDao.search(repoDao, token, false);

			List<Node> data = new ArrayList<Node>();
			for (org.edu_sharing.restservices.shared.NodeRef ref : search.getResult()) {
				data.add(NodeDao.getNode(repoDao, storeRef.getProtocol(), storeRef.getIdentifier(), ref.getId(), filter).asNode());
			}

			Pagination pagination = new Pagination();
			pagination.setFrom(search.getSkip());
			pagination.setCount(data.size());
			pagination.setTotal(search.getCount());

			SearchResult response = new SearchResult();
			response.setNodes(data);
			response.setPagination(pagination);
			response.setFacettes(search.getFacettes());
			return Response.status(Response.Status.OK).entity(response).build();

		} catch (DAOException e) {
			return ErrorResponse.createResponse(e);
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}

	@OPTIONS
	@Path("/lucene")
	@ApiOperation(hidden = true, value = "")

	public Response options03() {

		return Response.status(Response.Status.OK).header("Allow", "OPTIONS, GET").build();
	}

	@GET
	@Path("/lucene/export")
	@Consumes({ "application/json" })

	@ApiOperation(value = "Search for custom lucene query and choose specific properties to load", notes = "e.g. @cm\\:name:\"*\"")

	@ApiResponses(value = { @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = List.class),
			@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
			@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
			@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
			@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
			@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) })

	public Response exportByLucene(
			@ApiParam(value = "query", defaultValue = "@cm\\:name:\"*\"") @QueryParam("query") String query,
			@ApiParam(value = RestConstants.MESSAGE_SORT_PROPERTIES) @QueryParam("sortProperties") List<String> sortProperties,
			@ApiParam(value = RestConstants.MESSAGE_SORT_ASCENDING) @QueryParam("sortAscending") List<Boolean> sortAscending,
			@ApiParam(value = "properties to fetch, use parent::<property> to include parent property values") @QueryParam("properties") List<String> properties,
			@ApiParam(value = "store, workspace or archive") @QueryParam("store") LuceneStore store,
			@Context HttpServletRequest req) {

		try {

			//check that there is an admin
			AdminServiceFactory.getInstance();
			RepositoryDao repoDao = RepositoryDao.getHomeRepository();


			List<Map<String,Serializable>> data = new ArrayList<>();
			//
			boolean haseMore = true;
			int pageSize = 1000;
			int page = 0;
			do{

				SearchToken token = new SearchToken();
				token.setSortDefinition(new SortDefinition(sortProperties, sortAscending));
				token.setFrom(page);
				token.setMaxResult(pageSize);
				token.setContentType(ContentType.ALL);
				token.setLuceneString(query);
				token.disableSearchCriterias();
				StoreRef storeRef = LuceneStore.Archive.equals(store) ? StoreRef.STORE_REF_ARCHIVE_SPACESSTORE : StoreRef.STORE_REF_WORKSPACE_SPACESSTORE;
				if (LuceneStore.Archive.equals(store)) {
					token.setStoreName(storeRef.getIdentifier());
					token.setStoreProtocol(storeRef.getProtocol());
				}
				NodeSearch search = NodeDao.search(repoDao, token, false);
				logger.info("page: "+ page +" count:"+search.getCount() +" t:"+Thread.currentThread().getId());
				page = page + pageSize;
				if((search.getCount() - 1) <= page){
					haseMore = false;
				}
				for (org.edu_sharing.restservices.shared.NodeRef ref : search.getResult()) {
					NodeRef alfRef=new NodeRef(storeRef, ref.getId());
					Map<String, Serializable> props=new HashMap<>();
					for(String prop : properties){
						if(prop.startsWith("parent::")){
							String parentId = NodeServiceFactory.getLocalService().getPrimaryParent(ref.getId());
							String realProp=prop.substring("parent::".length());
							props.put(prop, NodeServiceHelper.getPropertyNative(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,parentId),CCConstants.getValidGlobalName(realProp)));
						}
						else{
							props.put(prop, NodeServiceHelper.getPropertyNative(alfRef,CCConstants.getValidGlobalName(prop)));
						}
					}
					data.add(props);
				}

			}while(haseMore);

			logger.info("result:" + data.size());
			String tempDir = System.getProperty("java.io.tmpdir");
			String protocolFile = tempDir + "/" + "edu_export_lucene.txt";
			try (Writer writer = new FileWriter(protocolFile)) {
				Gson gson = new GsonBuilder().create();
				gson.toJson(data, writer);
			}
			//
			return Response.status(Response.Status.OK).entity(data).build();

		} catch (DAOException e) {
			return ErrorResponse.createResponse(e);
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}


	@PUT
	@Path("/deletePersons")
	@ApiOperation(value = "delete persons", notes = "delete the given persons. Their status must be set to \"todelete\"")
	@ApiResponses(value = { @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = PersonReport.class),
			@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
			@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
			@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
			@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
			@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) })
	public Response deletePerson(
			@ApiParam(value = "names of the users to delete", required = true) @QueryParam("username") List<String> username,
			@ApiParam(value = "options object what and how to delete user contents") PersonDeleteOptions options,
			@Context HttpServletRequest req) {
		try {
			PersonReport result=new PersonLifecycleService().deletePersons(username,options);
			return Response.ok().entity(result).build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}


    @POST
    @Path("/log")
    @ApiOperation(value = "Change the loglevel for classes at runtime.", notes = "Root appenders are used. Check the appender treshold.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Void.class),
            @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
            @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
            @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
            @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
            @ApiResponse(code = 409, message = RestConstants.HTTP_409, response = ErrorResponse.class),
            @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) })

	public Response changeLogging(
			@ApiParam(value = "name", required = true) @QueryParam("name") String name,
			@ApiParam(value = "loglevel", required = true) @QueryParam("loglevel") String loglevel,
			@ApiParam(value = "appender", defaultValue = "File") @QueryParam("appender") String appender,
			@Context HttpServletRequest req) {
		try {

			//check that there is an admin
			AdminServiceFactory.getInstance();

			if(name.startsWith("org.alfresco") ||
					name.startsWith("org.edu_sharing.alfresco") ||
					name.startsWith("org.edu_sharing.repository.server.tools.cache")) {
				ClassLoader clAlf = AlfAppContextGate.getApplicationContext().getClassLoader();

				Class<?> logManager = clAlf.loadClass("org.apache.log4j.LogManager");
				Method methodGetLogger = logManager.getMethod("getLogger", String.class);
				Object logger = methodGetLogger.invoke(null, name);

				Class<?> logLevelClass = clAlf.loadClass("org.apache.log4j.Level");
				Method methodToLevel = logLevelClass.getMethod("toLevel",String.class);
				Method methodSetLevel = logger.getClass().getMethod("setLevel",logLevelClass);
				methodSetLevel.invoke(logger, methodToLevel.invoke(null, loglevel));

				/*Method methodGetRootLogger = logger.getClass().getMethod("getRootLogger", null);
				Object rootLogger = methodGetRootLogger.invoke(null, null);

				Method methodGetAppender = rootLogger.getClass().getMethod("getAppender", String.class);
				Object appenderObj = methodGetAppender.invoke(rootLogger, appender);

				Class<?> priorityClass = clAlf.loadClass("org.apache.log4j.Priority");
				Method methodToPriority = priorityClass.getMethod("toPriority", String.class);
				Object priorityObj = methodToPriority.invoke(null, loglevel);
				Method methodSetTreshold = appenderObj.getClass().getMethod("setThreshold", priorityClass);
				methodSetTreshold.invoke(appenderObj, priorityObj);

				Method methodAddAppender = logger.getClass().getMethod("addAppender", clAlf.loadClass("org.apache.log4j.Appender"));
				methodAddAppender.invoke(logger, appenderObj);*/
            }else {
                Logger logger = LogManager.getLogger(name);
                logger.setLevel(Level.toLevel(loglevel));

                /**
                 * logger is auto added to root appender, so we don't need to do this, maybe we can check if the reshhold of the appender is enough for the logger
                 */
				/*Logger rootLogger = Logger.getRootLogger();
				System.out.println("rootLogger:" + rootLogger.getName());
				Appender appenderObj = rootLogger.getAppender(appender);
				System.out.println("appenderObj:" + appenderObj.getName() + " " + ((AppenderSkeleton)appenderObj).getThreshold() +" appenders:"+ logger.getAllAppenders().hasMoreElements());
				*/



                //	((AppenderSkeleton)appenderObj).setThreshold(Priority.toPriority(loglevel, Priority.INFO));
                //	logger.addAppender(appenderObj);
            }

            return Response.status(Response.Status.OK).build();
        } catch (NotAnAdminException e) {
            return ErrorResponse.createResponse(e);
        } catch (Exception e) {
            return ErrorResponse.createResponse(e);
        }

    }


	@GET
	@Path("/repositoryConfig")
	@ApiOperation(value = "get the repository config object")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = RepositoryConfig.class),
			@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
			@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
			@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
			@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
			@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) })
	public Response getConfig(@Context HttpServletRequest req) {
		try {
			return Response.ok().entity(AdminServiceFactory.getInstance().getConfig()).build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}
	@PUT
	@Path("/repositoryConfig")
	@ApiOperation(value = "set/update the repository config object")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Void.class),
			@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
			@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
			@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
			@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
			@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) })
	public Response setConfig(@Context HttpServletRequest req,RepositoryConfig config) {
		try {
			AdminServiceFactory.getInstance().setConfig(config);
			return Response.ok().build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}
	@GET
	@Path("/configFile")
	@ApiOperation(value = "get a base system config file (e.g. edu-sharing.conf)")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = String.class),
			@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
			@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
			@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
			@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
			@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) })
	public Response getConfigFile(@Context HttpServletRequest req,
								  @ApiParam(value = "filename to fetch", required = true) @QueryParam("filename") String filename
								  ) {
		try {
			String content=AdminServiceFactory.getInstance().getConfigFile(filename);
			return Response.ok().entity(content).build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}
	@PUT
	@Path("/configFile")
	@ApiOperation(value = "update a base system config file (e.g. edu-sharing.conf)")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Void.class),
			@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
			@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
			@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
			@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
			@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) })
	public Response updateConfigFile(@Context HttpServletRequest req,
									 @ApiParam(value = "filename to fetch", required = true) @QueryParam("filename") String filename,
									 String content) {
		try {
			AdminServiceFactory.getInstance().updateConfigFile(filename,content);
			return Response.ok().build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}

	public enum LuceneStore {
		Workspace,
		Archive
	}
}
