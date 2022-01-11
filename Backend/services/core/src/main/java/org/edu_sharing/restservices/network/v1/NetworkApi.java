package org.edu_sharing.restservices.network.v1;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import io.swagger.annotations.*;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.restservices.*;
import org.edu_sharing.restservices.network.v1.model.RepoEntries;
import org.edu_sharing.restservices.node.v1.model.WorkflowHistory;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.edu_sharing.restservices.shared.Repo;
import org.edu_sharing.service.network.model.Service;
import org.edu_sharing.service.network.model.StoredService;

@Path("/network/v1")
@Api(tags = {"NETWORK v1"})
@ApiService(value="NETWORK", major=1, minor=0)
public class NetworkApi {

    private static Logger logger = Logger.getLogger(NetworkApi.class);

    @GET
    @Path("/repositories")
    @ApiOperation(
            value = "Get repositories.",
            notes = "Get repositories.")
    @ApiResponses(
            value = {
                    @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = RepoEntries[].class),
                    @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
                    @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
                    @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
                    @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
                    @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class)
            })

    public Response getRepositories(
            @Context HttpServletRequest req) {

        try {

            List<Repo> repos = new ArrayList<Repo>();
            for (RepositoryDao repository : RepositoryDao.getRepositories()) {
                repos.add(repository.asRepo());
            }

            RepoEntries response = new RepoEntries();
            response.setList(repos);

            return Response.status(Response.Status.OK).entity(response).build();

        } catch (Throwable t) {
            return ErrorResponse.createResponse(t);
        }

    }

    @OPTIONS
    @Path("/repositories")
    @ApiOperation(hidden = true, value = "")

    public Response options() {

        return Response.status(Response.Status.OK).header("Allow", "OPTIONS, GET").build();
    }
    @GET
    @Path("/service")
    @ApiOperation(
            value = "Get own service.",
            notes = "Get the servic entry from the current repository.")
    @ApiResponses(
            value = {
                    @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = StoredService.class),
                    @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
                    @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
                    @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
                    @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
                    @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class)
            })
    public Response getService(
            @Context HttpServletRequest req
    ) {
        try {
            StoredService response = NetworkDao.getOwnService();
            return Response.status(Response.Status.OK).entity(response).build();
        } catch (Throwable t) {
            return ErrorResponse.createResponse(t);
        }
    }
    @GET
    @Path("/services")
    @ApiOperation(
            value = "Get services.",
            notes = "Get registerted services.")
    @ApiResponses(
            value = {
                    @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = StoredService[].class),
                    @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
                    @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
                    @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
                    @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
                    @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class)
            })
    public Response getServices(
            @Context HttpServletRequest req,
            @ApiParam(value = "search or filter for services", defaultValue = "", required = false) @QueryParam("query") String query
            ) {
        try {
            Collection<StoredService> response = NetworkDao.getServices();
            return Response.status(Response.Status.OK).entity(response).build();
        } catch (Throwable t) {
            return ErrorResponse.createResponse(t);
        }
    }

    @POST
    @Path("/services")
    @ApiOperation(
            value = "Register service.",
            notes = "Register a new service.")
    @ApiResponses(
            value = {
                    @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = StoredService.class),
                    @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
                    @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
                    @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
                    @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
                    @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class)
            })
    public Response addService(
            @Context HttpServletRequest req,
            @ApiParam(value = "Service data object") Service service
    ) {
        try {
            StoredService response = NetworkDao.addService(service);
            return Response.status(Response.Status.OK).entity(response).build();
        } catch (Throwable t) {
            return ErrorResponse.createResponse(t);
        }
    }
    @PUT
    @Path("/services/{id}")
    @ApiOperation(
            value = "Update a service.",
            notes = "Update an existing service.")
    @ApiResponses(
            value = {
                    @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = StoredService.class),
                    @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
                    @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
                    @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
                    @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
                    @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class)
            })
    public Response updateService(
            @Context HttpServletRequest req,
            @ApiParam(value = "Service id", required=true ) @PathParam("id") String id,
            @ApiParam(value = "Service data object") Service service
    ) {
        try {
            StoredService response = NetworkDao.updateService(id,service);
            return Response.status(Response.Status.OK).entity(response).build();
        } catch (Throwable t) {
            return ErrorResponse.createResponse(t);
        }
    }
}
