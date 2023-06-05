package org.edu_sharing.restservices.notification.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.edu_sharing.restservices.ApiService;
import org.edu_sharing.restservices.DAOException;
import org.edu_sharing.restservices.RestConstants;
import org.edu_sharing.restservices.login.v1.model.Login;
import org.edu_sharing.service.notification.NotificationConfig;
import org.edu_sharing.service.notification.NotificationServiceFactoryUtility;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

@Path("/notification/v1")
@Tag(name="NOTIFICATION v1")
@ApiService(value="NOTIFICATION", major=1, minor=0)
@Consumes({ "application/json" })
@Produces({"application/json"})
public class NotificationApi {

    @GET
    @Path("/config")
    @Operation(summary = "get the config for notifications of the current user")
    @ApiResponses(
            value = {
                    @ApiResponse(responseCode = "200", description = RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = NotificationConfig.class))),
            })

    public Response getConfig(@Context HttpServletRequest req) throws DAOException {
        try {
            return Response.ok(NotificationServiceFactoryUtility.getLocalService().getConfig()).build();
        }catch(Throwable t) {
            throw DAOException.mapping(t);
        }
    }
    @PUT
    @Path("/config")
    @Operation(summary = "Update the config for notifications of the current user")
    @ApiResponses(
            value = {
                    @ApiResponse(responseCode = "200", description = RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = Void.class))),
            })

    public Response setConfig(@Context HttpServletRequest req, NotificationConfig config) throws DAOException {
        try {
            NotificationServiceFactoryUtility.getLocalService().setConfig(config);
            return Response.ok().build();
        } catch (Throwable t) {
            throw DAOException.mapping(t);
        }
    }
}