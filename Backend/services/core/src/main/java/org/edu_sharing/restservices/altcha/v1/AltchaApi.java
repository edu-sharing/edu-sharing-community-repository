package org.edu_sharing.restservices.altcha.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.edu_sharing.restservices.ApiService;
import org.edu_sharing.restservices.RestConstants;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.edu_sharing.service.altcha.AltchaService;
import org.springframework.beans.factory.annotation.Autowired;

@Path("/altcha/v1")
@Tag(name = "ALTCHA v1")
@ApiService(value = "ALTCHA", major = 1, minor = 0)
@SecurityRequirements
@Produces({MediaType.APPLICATION_JSON})
public class AltchaApi {

    @Autowired
    private AltchaService altchaService;

    @GET
    @Path("/challenge")
    @Operation(summary = "Get a new ALTCHA challenge.", description = "Proof-of-work challenge which guests have to solve before reporting a node. Returns 204 if ALTCHA is disabled.")
    @ApiResponses(
            value = {
                    @ApiResponse(responseCode = "200", description = RestConstants.HTTP_200, content = @Content(schema = @Schema(type = "object"))),
                    @ApiResponse(responseCode = "204", description = "ALTCHA is disabled."),
                    @ApiResponse(responseCode = "500", description = RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            })
    public Response getChallenge(@Context HttpServletRequest req) {
        try {
            if (!altchaService.isEnabled()) {
                return Response.noContent().build();
            }
            CacheControl noStore = new CacheControl();
            noStore.setNoStore(true);
            return Response.ok(altchaService.createChallenge(AltchaService.ACTION_REPORT, req.getRemoteAddr()).toJson())
                    .cacheControl(noStore)
                    .build();
        } catch (Throwable t) {
            return ErrorResponse.createResponse(t);
        }
    }
}
