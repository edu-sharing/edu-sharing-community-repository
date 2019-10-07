package org.edu_sharing.restservices.mediacenter.v1;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.restservices.ApiService;
import org.edu_sharing.restservices.GroupDao;
import org.edu_sharing.restservices.MediacenterDao;
import org.edu_sharing.restservices.RepositoryDao;
import org.edu_sharing.restservices.RestConstants;
import org.edu_sharing.restservices.admin.v1.model.ExcelResult;
import org.edu_sharing.restservices.mediacenter.v1.model.MediacentersImportResult;
import org.edu_sharing.restservices.mediacenter.v1.model.OrganisationsImportResult;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.edu_sharing.restservices.shared.Group;
import org.edu_sharing.restservices.shared.Mediacenter;
import org.edu_sharing.service.admin.AdminServiceFactory;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.service.mediacenter.MediacenterServiceFactory;
import org.glassfish.jersey.media.multipart.FormDataParam;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Path("/mediacenter/v1")
@Api(tags = {"MEDIACENTER v1"})
@ApiService(value="MEDIACENTER", major=1, minor=0)
public class MediacenterApi {

	private static Logger logger = Logger.getLogger(MediacenterApi.class);


	@GET
	@Path("/mediacenter/{repository}")

	@ApiOperation(
			value = "get mediacenters in the repository.",
			notes = "Only shows the one available/managing the current user (only admin can access all)"
	)

	@ApiResponses(
			value = {
					@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Mediacenter[].class),
					@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
					@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
					@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
					@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
					@ApiResponse(code = 409, message = RestConstants.HTTP_409, response = ErrorResponse.class),
					@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class)
			})

	public Response getMediacenters(
			@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID,required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
			@Context HttpServletRequest req) {

		try {

			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			List<Mediacenter> mediacenters = MediacenterDao.getAll(repoDao).stream().map(MediacenterDao::asMediacenter).collect(Collectors.toList());
			return Response.status(Response.Status.OK).entity(mediacenters).build();

		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}

	}
    @POST
    @Path("/mediacenter/{repository}/{mediacenter}")

    @ApiOperation(
    	value = "create new mediacenter in repository.",
		notes = "admin rights are required."
	)

    @ApiResponses(
    	value = {
    		@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Mediacenter.class),
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
	        @ApiResponse(code = 409, message = RestConstants.HTTP_409, response = ErrorResponse.class),
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class)
    	})

    public Response createMediacenter(
        	@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID,required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
    		@ApiParam(value = "mediacenter name",required=true) @PathParam("mediacenter") String mediacenter,
    		Mediacenter.Profile profile,
    		@Context HttpServletRequest req) {

    	try {

	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	Mediacenter group = MediacenterDao.create(repoDao,mediacenter,profile).asMediacenter();
	    	return Response.status(Response.Status.OK).entity(group).build();

    	} catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}

    }
	@PUT
	@Path("/mediacenter/{repository}/{mediacenter}")

	@ApiOperation(
			value = "edit a mediacenter in repository."
	)

	@ApiResponses(
			value = {
					@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Mediacenter.class),
					@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
					@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
					@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
					@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
					@ApiResponse(code = 409, message = RestConstants.HTTP_409, response = ErrorResponse.class),
					@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class)
			})

	public Response editMediacenter(
			@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID,required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
			@ApiParam(value = "mediacenter name",required=true) @PathParam("mediacenter") String mediacenter,
			Mediacenter.Profile profile,
			@Context HttpServletRequest req) {

		try {

			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			MediacenterDao group = MediacenterDao.get(repoDao,mediacenter);
			group.changeProfile(profile);
			return Response.status(Response.Status.OK).entity(group.asMediacenter()).build();

		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}

	}

	@GET
	@Path("/mediacenter/{repository}/{mediacenter}/manages")

	@ApiOperation(
			value = "get groups that are managed by the given mediacenter"
	)

	@ApiResponses(
			value = {
					@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Group[].class),
					@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
					@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
					@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
					@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
					@ApiResponse(code = 409, message = RestConstants.HTTP_409, response = ErrorResponse.class),
					@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class)
			})

	public Response getMediacenterGroups(
			@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID,required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
			@ApiParam(value = "authorityName of the mediacenter that should manage the group",required=true) @PathParam("mediacenter") String mediacenter,
			@Context HttpServletRequest req) {

		try {
			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			MediacenterDao dao = MediacenterDao.get(repoDao, mediacenter);
			List<Group> groups = dao.getManagedGroups().stream().map(GroupDao::asGroup).collect(Collectors.toList());
			return Response.status(Response.Status.OK).entity(groups).build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}

	}
	@PUT
	@Path("/mediacenter/{repository}/{mediacenter}/manages/{group}")

	@ApiOperation(
			value = "add a group that is managed by the given mediacenter",
			notes = "although not restricted, it is recommended that the group is an edu-sharing organization (admin rights are required)")

	@ApiResponses(
			value = {
					@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Group[].class),
					@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
					@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
					@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
					@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
					@ApiResponse(code = 409, message = RestConstants.HTTP_409, response = ErrorResponse.class),
					@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class)
			})

	public Response addMediacenterGroup(
			@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID,required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
			@ApiParam(value = "authorityName of the mediacenter that should manage the group",required=true) @PathParam("mediacenter") String mediacenter,
			@ApiParam(value = "authorityName of the group that should be managed by that mediacenter",required=true) @PathParam("group") String group,
			@Context HttpServletRequest req) {

		try {

			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			MediacenterDao dao = MediacenterDao.get(repoDao, mediacenter);
			dao.addManagedGroup(group);
			List<Group> groups = dao.getManagedGroups().stream().map(GroupDao::asGroup).collect(Collectors.toList());
			return Response.status(Response.Status.OK).entity(groups).build();

		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}

	}
	@DELETE
	@Path("/mediacenter/{repository}/{mediacenter}/manages/{group}")

	@ApiOperation(
			value = "delete a group that is managed by the given mediacenter",
			notes = "admin rights are required."
	)

	@ApiResponses(
			value = {
					@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Group[].class),
					@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
					@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
					@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
					@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
					@ApiResponse(code = 409, message = RestConstants.HTTP_409, response = ErrorResponse.class),
					@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class)
			})

	public Response removeMediacenterGroup(
			@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID,required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
			@ApiParam(value = "authorityName of the mediacenter that should manage the group",required=true) @PathParam("mediacenter") String mediacenter,
			@ApiParam(value = "authorityName of the group that should not longer be managed by that mediacenter",required=true) @PathParam("group") String group,
			@Context HttpServletRequest req) {

		try {

			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			MediacenterDao dao = MediacenterDao.get(repoDao, mediacenter);
			dao.removeManagedGroup(group);
			List<Group> groups = dao.getManagedGroups().stream().map(GroupDao::asGroup).collect(Collectors.toList());
			return Response.status(Response.Status.OK).entity(groups).build();

		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}

	}
	
	@POST
	@Path("/import/mediacenters")

	@ApiOperation(value = "Import mediacenters", notes = "Import mediacenters.")

	@ApiResponses(value = { @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = MediacentersImportResult.class),
			@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
			@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
			@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
			@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
			@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) })
	public Response importMediacenters(@ApiParam(value = "Mediacenters csv to import", required = true) @FormDataParam("mediacenters") InputStream is,
			@Context HttpServletRequest req) {
		try {

			org.edu_sharing.service.authority.AuthorityService eduAuthorityService = AuthorityServiceFactory.getAuthorityService(ApplicationInfoList.getHomeRepository().getAppId());
			if(!eduAuthorityService.isGlobalAdmin()){
				throw new Exception("Admin rights are required for this endpoint");
			}

			int count = MediacenterServiceFactory.getInstance().importMediacenters(is);
			MediacentersImportResult result = new MediacentersImportResult();
			result.setRows(count);
			return Response.ok().entity(result).build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}

	@POST
	@Path("/import/organisations")

	@ApiOperation(value = "Import Organisations", notes = "Import Organisations.")

	@ApiResponses(value = { @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = OrganisationsImportResult.class),
			@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
			@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
			@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
			@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
			@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) })
	public Response importOrganisations(@ApiParam(value = "Organisations csv to import", required = true) @FormDataParam("organisations") InputStream is,
									   @Context HttpServletRequest req) {
		try {

			org.edu_sharing.service.authority.AuthorityService eduAuthorityService = AuthorityServiceFactory.getAuthorityService(ApplicationInfoList.getHomeRepository().getAppId());
			if(!eduAuthorityService.isGlobalAdmin()){
				throw new Exception("Admin rights are required for this endpoint");
			}

			int count = MediacenterServiceFactory.getInstance().importOrganisations(is);
			OrganisationsImportResult result = new OrganisationsImportResult();
			result.setRows(count);
			return Response.ok().entity(result).build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}

}

