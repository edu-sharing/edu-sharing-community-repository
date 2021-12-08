package org.edu_sharing.restservices.lti.v13;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.restservices.RestConstants;
import org.edu_sharing.restservices.node.v1.model.WorkflowHistory;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.edu_sharing.service.lti13.LTIConstants;
import org.edu_sharing.service.lti13.LTIException;
import org.edu_sharing.service.lti13.LTIOidcUtil;
import org.edu_sharing.service.lti13.LTIService;
import org.edu_sharing.service.lti13.model.LoginInitiationDTO;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;

@Path("/lti/v13")
@Consumes({ "application/json" })
@Produces({"application/json"})
@Tag(name="LTI v13")
public class LTIApi {

    Logger logger = Logger.getLogger(LTIApi.class);

    @POST
    @Path("/oidc/login_initiations")
    @Operation(summary = "lti authentication process preparation.", description = "preflight phase. prepares lti authentication process. checks it issuer is valid")

    @ApiResponses(
            value = {
                    @ApiResponse(responseCode="200", description= RestConstants.HTTP_200, content = @Content(mediaType = "text/html", schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(mediaType = "text/html", schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(mediaType = "text/html", schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(mediaType = "text/html", schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(mediaType = "text/html", schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(mediaType = "text/html", schema = @Schema(implementation = String.class)))
            })
    public Response loginInitiations(@Parameter(description = "Issuer of the request, will be validated",required=true) @QueryParam(LTIConstants.LTI_PARAM_ISS) String iss,
                                 @Parameter(description = "target url of platform at the end of the flow",required=true) @QueryParam(LTIConstants.LTI_PARAM_TARGET_LINK_URI) String targetLinkUrl,
                                 @Parameter(description = "Id of the issuer",required=false) @QueryParam(LTIConstants.LTI_PARAM_CLIENT_ID) String clientId,
                                 @Parameter(description = "context information of the platform",required=false) @QueryParam(LTIConstants.LTI_PARAM_LOGIN_HINT) String loginHint,
                                 @Parameter(description = "additional context information of the platform",required=false) @QueryParam(LTIConstants.LTI_PARAM_MESSAGE_HINT) String ltiMessageHint,
                                 @Parameter(description = "A can have multiple deployments in a platform",required=false) @QueryParam(LTIConstants.LTI_PARAM_DEPLOYMENT_ID) String ltiDeploymentId,
                                 @Context HttpServletRequest req
                                 ){
        /**
         * @TODO check if multiple deployments got an own key pair
         */

        LoginInitiationDTO dto = new LoginInitiationDTO(iss,loginHint,targetLinkUrl,ltiMessageHint,clientId,ltiDeploymentId);
        LTIService ltiService = new LTIService();
        try {
            /**
             * @TODO maybe use db instead of applicationinfo
             */
            ApplicationInfo applicationInfo = ltiService.getApplicationInfo(iss, clientId, ltiDeploymentId);
            Map<String, String> model = new LTIOidcUtil().generateAuthRequestPayload(applicationInfo, dto);
            return Response.status(Response.Status.OK).entity(getHTML(applicationInfo.getLtiOidc(),model,null)).build();
        } catch (LTIException | GeneralSecurityException | IOException e) {
            return Response.status(Response.Status.OK).entity(getHTML(null,null,e.getMessage())).build();
        }
    }

    /**
     * @TODO use template engine?
     * @param formTargetUrl
     * @param params
     * @return
     */
    private String getHTML(String formTargetUrl, Map<String,String> params, String errorMessage){
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body>");
        if(errorMessage == null) {
            sb.append("<form action=\"" + formTargetUrl + "\" method=\"post\"");
            for (Map.Entry<String, String> entry : params.entrySet()) {
                sb.append("<input type=\"hidden\" id=\"" + entry.getKey() + "\" name=\"" + entry.getKey() + "\" value=\"" + entry.getValue() + "\" class=\"form-control\"/>");
            }
            sb.append("<input type=\"submit\" value=\"Submit POST\" class=\"btn btn-primary\">")
                    .append("</form>");
        }else {
            sb.append("error:"+ errorMessage);
        }
        sb.append("</body></html>");
        return sb.toString();
    }


    @POST
    @Path("/" + LTIConstants.LTI_TOOL_REDIRECTURL_PATH)
    @Operation(summary = "lti authentication process preparation.", description = "preflight phase. prepares lti authentication process. checks it issuer is valid")

    @ApiResponses(
            value = {
                    @ApiResponse(responseCode="200", description= RestConstants.HTTP_200, content = @Content(mediaType = "text/html", schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(mediaType = "text/html", schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(mediaType = "text/html", schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(mediaType = "text/html", schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(mediaType = "text/html", schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(mediaType = "text/html", schema = @Schema(implementation = String.class)))
            })
    public Response lti(@Parameter(description = "Issuer of the request, will be validated",required=true) @QueryParam("id_token") String idToken,
                        @Parameter(description = "Issuer of the request, will be validated",required=true) @QueryParam("state") String state,
                        @Context HttpServletRequest req){
        logger.info("id_token:"+idToken +" state:"+state);
        return Response.status(Response.Status.OK).build();
    }
}
