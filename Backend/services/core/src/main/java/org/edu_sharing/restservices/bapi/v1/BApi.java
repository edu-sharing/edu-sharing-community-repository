package org.edu_sharing.restservices.bapi.v1;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import org.edu_sharing.service.bapi.BApiProxyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;

@Hidden
@Path("/bapi")
public class BApi {

    @Autowired
    private BApiProxyService bapiProxyService;

    @Context
    private HttpHeaders headers;

    @Context
    private UriInfo uriInfo;

    @Path("{path: .*}")
    @GET
    public Response proxyGet(@PathParam("path") String path) {
        return bapiProxyService.forwardRequest(path, null, headers, uriInfo.getRequestUri().getQuery(), HttpMethod.GET);
    }

    @Path("{path: .*}")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response proxyPost(@PathParam("path") String path, String body) {
        return bapiProxyService.forwardRequest(path, body, headers, uriInfo.getRequestUri().getQuery(), HttpMethod.POST);
    }

    @Path("{path: .*}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response proxyPut(@PathParam("path") String path, String body) {
        return bapiProxyService.forwardRequest(path, body, headers, uriInfo.getRequestUri().getQuery(), HttpMethod.PUT);
    }

    @Path("{path: .*}")
    @DELETE
    public Response proxyDelete(@PathParam("path") String path) {
        return bapiProxyService.forwardRequest(path, null, headers, uriInfo.getRequestUri().getQuery(), HttpMethod.DELETE);
    }
}