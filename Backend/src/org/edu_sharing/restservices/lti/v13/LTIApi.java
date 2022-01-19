package org.edu_sharing.restservices.lti.v13;

import com.nimbusds.jose.jwk.AsymmetricJWK;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import edu.uoc.elc.lti.tool.Tool;
import edu.uoc.elc.lti.tool.oidc.LoginRequest;
import edu.uoc.elc.spring.lti.security.openid.HttpSessionOIDCLaunchSession;
import edu.uoc.elc.spring.lti.security.openid.LoginRequestFactory;
import edu.uoc.elc.spring.lti.security.utils.TokenFactory;
import edu.uoc.elc.spring.lti.tool.ToolFactory;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.restservices.RestConstants;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.edu_sharing.restservices.shared.Repo;
import org.edu_sharing.service.admin.AdminServiceFactory;
import org.edu_sharing.service.authority.AuthorityServiceHelper;
import org.edu_sharing.service.lti13.*;
import org.edu_sharing.service.lti13.model.LTISessionObject;
import org.edu_sharing.service.lti13.model.LoginInitiationDTO;
import org.edu_sharing.service.lti13.uoc.Config;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/lti/v13")
@Consumes({ "text/html" })
@Produces({"text/html"})
@Tag(name="LTI v13")
public class LTIApi {

    Logger logger = Logger.getLogger(LTIApi.class);

    @POST
    @Path("/oidc/login_initiations")
    @Operation(summary = "lti authentication process preparation.", description = "preflight phase. prepares lti authentication process. checks it issuer is valid")
    @Consumes({"application/x-www-form-urlencoded"})
    @ApiResponses(
            value = {
                    @ApiResponse(responseCode="200", description= RestConstants.HTTP_200, content = @Content(mediaType = "text/html", schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(mediaType = "text/html", schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(mediaType = "text/html", schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(mediaType = "text/html", schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(mediaType = "text/html", schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(mediaType = "text/html", schema = @Schema(implementation = String.class)))
            })
    public Response loginInitiations(@Parameter(description = "Issuer of the request, will be validated",required=true) @FormParam(LTIConstants.LTI_PARAM_ISS) String iss,
                                 @Parameter(description = "target url of platform at the end of the flow",required=true) @FormParam(LTIConstants.LTI_PARAM_TARGET_LINK_URI) String targetLinkUrl,
                                 @Parameter(description = "Id of the issuer",required=false) @FormParam(LTIConstants.LTI_PARAM_CLIENT_ID) String clientId,
                                 @Parameter(description = "context information of the platform",required=false) @FormParam(LTIConstants.LTI_PARAM_LOGIN_HINT) String loginHint,
                                 @Parameter(description = "additional context information of the platform",required=false) @FormParam(LTIConstants.LTI_PARAM_MESSAGE_HINT) String ltiMessageHint,
                                 @Parameter(description = "A can have multiple deployments in a platform",required=false) @FormParam(LTIConstants.LTI_PARAM_DEPLOYMENT_ID) String ltiDeploymentId,
                                 @Context HttpServletRequest req
                                 ){
        /**
         * @TODO check if multiple deployments got an own key pair
         */

        RepoTools repoTools = new RepoTools();
        try {
            ApplicationInfo platform = repoTools.getApplicationInfo(iss, clientId, ltiDeploymentId);
            Tool tool = Config.getTool(platform,req,true);
            // get data from request
            final LoginRequest loginRequest = LoginRequestFactory.from(req);
            if (this.logger.isInfoEnabled()) {
                this.logger.info("OIDC launch received with " + loginRequest.toString());
            }
            final URI uri = new URI(loginRequest.getTarget_link_uri());
			/* commented in local because localhost resolves to 0:0:0:0
			if (!uri.getHost().equals(request.getRemoteHost())) {
				throw new ServletException("Bad request");
			}
			*/

            // do the redirection
            String authRequest = tool.getOidcAuthUrl(loginRequest);
            //response.sendRedirect(authRequest);
            return Response.status(302).location(new URI(authRequest)).build();

        } catch (Throwable e) {
            logger.error(e.getMessage(),e);
            return Response.status(Response.Status.OK).entity(getHTML(null,null,e.getMessage())).build();
        }
    }

    /**
     * store multiple for allowing multiple deployments of edu-sharing(ltitool) in lms(lti platform)
     * @param session
     * @param params
     */
    private void storeInSession(HttpSession session, Map<String,String> params){
        for(Map.Entry<String,String> entry : params.entrySet()){
            List<String> list = (List<String>)session.getAttribute(entry.getKey());
            if(list == null){
               list = new ArrayList<>();
            }
            list.add(entry.getValue());
            session.setAttribute(entry.getKey(), list);
        }
    }

    /**
     * @TODO use template engine?
     * @param formTargetUrl
     * @param params
     * @return
     */
    private String getHTML(String formTargetUrl, Map<String,String> params, String errorMessage){
        String FORMNAME = "ltiform";
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body>");
        sb.append("<script type=\"text/javascript\">window.onload=function(){document.forms[\""+FORMNAME+"\"].submit();}</script>");
        if(errorMessage == null) {
            sb.append("<form action=\"" + formTargetUrl + "\" method=\"post\" name=\"" + FORMNAME + "\"");
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
    @Operation(summary = "lti tool redirect.", description = "lti tool redirect")

    @Consumes({ "application/x-www-form-urlencoded" })
    @ApiResponses(
            value = {
                    @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(mediaType = "text/html", schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(mediaType = "text/html", schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(mediaType = "text/html", schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(mediaType = "text/html", schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(mediaType = "text/html", schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(mediaType = "text/html", schema = @Schema(implementation = String.class)))
            })
    public Response lti(@Parameter(description = "Issuer of the request, will be validated",required=true) @FormParam("id_token") String idToken,
                        @Parameter(description = "Issuer of the request, will be validated",required=true) @FormParam("state") String state,
                        @Context HttpServletRequest req){
        logger.info("id_token:"+idToken +" state:"+state);


        try{
            if(state == null) {
                throw new IllegalStateException("no state param provided");
            }

            if (idToken == null) {
                String message = "The request is not a LTI request, so no credentials at all. Returning current credentials";
                this.logger.error(message);
                throw new IllegalStateException(message);
            }

            /**
             * get claims cause we need clientId,deploymentId,iss for applicationinfo of plattform to instance tool
             * token will be validated with public key of the platform app
             * @TODO use keyset url
             */
            LTIJWTUtil ltijwtUtil = new LTIJWTUtil();
            Jws<Claims> jws = ltijwtUtil.validateJWT(idToken);

            /**
             * validate nonce
             */
            String nonce = jws.getBody().get("nonce", String.class);
            String sessionNonce = new HttpSessionOIDCLaunchSession(req).getNonce();
            if(!nonce.equals(sessionNonce)){
                throw new IllegalStateException("nonce is invalid");
            }

            Tool tool = Config.getTool(ltijwtUtil.getPlatform(),req,true);

            /**
             * Launch validation: validates authentication response, and specific message(deeplink,....) validation
             * https://www.imsglobal.org/spec/security/v1p0/#authentication-response-validation
             */
            tool.validate(idToken, state);
            if (!tool.isValid()) {
                logger.error(tool.getReason());
                throw new IllegalStateException(tool.getReason());
            }


            /*List<String> sessionStates = (List<String>)req.getSession().getAttribute(LTIConstants.LTI_TOOL_SESS_ATT_STATE);
            if(sessionStates == null){
                throw new IllegalStateException("no states initiated for this session");
            }

            if(!sessionStates.contains(state)){
                throw new IllegalStateException("LTI request doesn't contains the expected state");
            }*/




            if(StringUtils.hasText(idToken)){
                //Now we validate the JWT token
                if (jws != null) {

                    /**
                     * edu-sharing authentication
                     */
                    String user = jws.getBody().getSubject();
                    String name = jws.getBody().get(LTIConstants.LTI_NAME, String.class);
                    String familyName = jws.getBody().get(LTIConstants.LTI_FAMILY_NAME, String.class);
                    String givenName = jws.getBody().get(LTIConstants.LTI_GIVEN_NAME, String.class);
                    String email = jws.getBody().get(LTIConstants.LTI_EMAIL, String.class);

                    String authenticatedUsername = RepoTools.authenticate(req,
                            RepoTools.mapToSSOMap(user, givenName, familyName, email));


                    /**
                     * safe to session for later usage
                     */
                    String ltiMessageType = jws.getBody().get(LTIConstants.LTI_MESSAGE_TYPE,String.class);
                    LTISessionObject ltiSessionObject = new LTISessionObject();
                    ltiSessionObject.setDeploymentId(jws.getBody().get(LTIConstants.LTI_DEPLOYMENT_ID,String.class));
                    ltiSessionObject.setIss(jws.getBody().get(LTIConstants.LTI_PARAM_ISS,String.class));
                    ltiSessionObject.setNonce(jws.getBody().get(LTIConstants.LTI_NONCE,String.class));
                    ltiSessionObject.setMessageType(ltiMessageType);
                    ltiSessionObject.setEduSharingAppId(new RepoTools().getAppId(ltiSessionObject.getIss(),
                            jws.getBody().getAudience(),
                            ltiSessionObject.getDeploymentId()));

                    /**
                     * @TODO: what happens when user is using the sames session within two browser windows
                     * maybe use list of LTISessionObject's
                     */
                    req.getSession().setAttribute(LTISessionObject.class.getName(),ltiSessionObject);
                    if(ltiMessageType.equals(LTIConstants.LTI_MESSAGE_TYPE_DEEP_LINKING)){
                        if(jws.getBody().containsKey(LTIConstants.DEEP_LINKING_SETTINGS)){
                            Map deepLinkingSettings = jws.getBody().get(LTIConstants.DEEP_LINKING_SETTINGS, Map.class);
                            ltiSessionObject.setDeepLinkingSettings(deepLinkingSettings);
                        }
                        /**
                         * @TODO check if this kind of redirect works
                         */

                        //return Response.status(302).location(new URI(ApplicationInfoList.getHomeRepository().getClientBaseUrl()+"/edu-sharing/components/search")).build();
                        return Response.seeOther(new URI(ApplicationInfoList.getHomeRepository().getClientBaseUrl()+"/components/search")).build();
                        //return Response.temporaryRedirect(new URI("/edu-sharing/components/search")).build();
                    }else if(ltiMessageType.equals(LTIConstants.LTI_MESSAGE_TYPE_RESOURCE_LINK)){
                        //rendering stuff
                        /**
                         * @TODO check for launch_presentation
                         * "https://purl.imsglobal.org/spec/lti/claim/launch_presentation": {
                         *     "locale": "en",
                         *     "document_target": "iframe",
                         *     "return_url": "http://localhost/moodle/mod/lti/return.php?course=2&launch_container=3&instanceid=1&sesskey=q6noraEPlA"
                         *   }
                         */
                        String targetLink = jws.getBody().get(LTIConstants.LTI_TARGET_LINK_URI, String.class);
                        return Response.seeOther(new URI(targetLink)).build();
                        //return Response.temporaryRedirect(new URI(targetLink)).build();
                    }else{
                        String message = "can not handle message type:" + ltiMessageType;
                        logger.error(message);
                        return Response.status(Response.Status.OK).entity(getHTML(null,null,message)).build();
                    }
                }
            }
        }catch(Exception e){
            logger.error(e.getMessage(),e);
            return Response.status(Response.Status.OK).entity(getHTML(null,null,e.getMessage())).build();
        }


        return Response.status(Response.Status.OK).build();
    }

    @POST
    @Path("/registration")
    @Operation(summary = "register LTI platform")
    @Consumes({ "application/json" })
    @Produces({ "application/json"})
    @ApiResponses(value = {
            @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = Void.class))),
            @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })
    public Response registerTest(@Parameter(description = "the issuer",required=true) @QueryParam("platformId") String platformId,
                                 @Parameter(description = "client id",required=true) @QueryParam("client_id") String clientId,
                                 @Parameter(description = "deployment id",required=true)  @QueryParam("deployment_id") String deploymentId,
                                 @Parameter(description = "oidc endpoint, authentication request url",required=true) @QueryParam("authentication_request_url") String authenticationRequestUrl,
                                 @Parameter(description = "jwks endpoint, keyset url",required=true) @QueryParam("keyset_url") String keysetUrl,
                                 @Parameter(description = "jwks key id",required=false) @QueryParam("key_id") String keyId,
                                 @Parameter(description = "auth token url",required=true) @QueryParam("auth_token_url") String authTokenUrl,
                                 @Context HttpServletRequest req
    ){
        try {
            registerPlatform(platformId,clientId,deploymentId,authenticationRequestUrl,keysetUrl,keyId,authTokenUrl);
            return Response.ok().build();
        } catch (Throwable t) {
            return ErrorResponse.createResponse(t);
        }
    }

    public static enum LTI_Plattforms {moodle};

    @POST
    @Path("/registration/{type}")
    @Operation(summary = "register LTI platform")
    @Consumes({ "application/json" })
    @Produces({ "application/json"})
    @ApiResponses(value = {
            @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = Void.class))),
            @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })
    public Response registerByType(@Parameter(description = "lti platform typ i.e. moodle", required=true) @PathParam("type") LTI_Plattforms type,
                                   @Parameter(description = "base url i.e. http://localhost/moodle used as platformId",required=true) @QueryParam("baseUrl") String baseUrl,
                                   @Parameter(description = "client id",required=false) @QueryParam("client_id") String clientId,
                                   @Parameter(description = "deployment id",required=false) @QueryParam("deployment_id") String deploymentId,
                                   @Context HttpServletRequest req
    ){
        try {

            registerPlatform(baseUrl,clientId,deploymentId,
                    baseUrl + LTIConstants.MOODLE_AUTHENTICATION_REQUEST_URL_PATH,
                    baseUrl + LTIConstants.MOODLE_KEYSET_URL_PATH,
                    null,
                    baseUrl+LTIConstants.MOODLE_AUTH_TOKEN_URL_PATH);
            return Response.ok().build();
        } catch (Throwable t) {
            return ErrorResponse.createResponse(t);
        }
    }


    private void registerPlatform(String platformId,
                                  String clientId, String deploymentId,
                                  String authenticationRequestUrl,
                                  String keysetUrl,
                                  String keyId,
                                  String authTokenUrl) throws Exception{
        if(!AuthorityServiceHelper.isAdmin()){
            throw new Exception("must be an admin to register lti platforms");
        }
        HashMap<String,String> properties = new HashMap<>();
        properties.put(ApplicationInfo.KEY_APPID, new RepoTools().getAppId(platformId,clientId,deploymentId));
        properties.put(ApplicationInfo.KEY_TYPE, "lti");
        properties.put(ApplicationInfo.KEY_LTI_DEPLOYMENT_ID, deploymentId);
        properties.put(ApplicationInfo.KEY_LTI_ISS, platformId);
        properties.put(ApplicationInfo.KEY_LTI_CLIENT_ID, clientId);
        properties.put(ApplicationInfo.KEY_LTI_OIDC_ENDPOINT, authenticationRequestUrl);
        properties.put(ApplicationInfo.KEY_LTI_AUTH_TOKEN_ENDPOINT,authTokenUrl);
        properties.put(ApplicationInfo.KEY_LTI_KEYSET_URL,keysetUrl);

        JWKSet publicKeys = JWKSet.load(new URL(keysetUrl));
        if(publicKeys == null){
            throw new Exception("no public key found");
        }
        JWK jwk = (keyId == null) ? publicKeys.getKeys().get(0) :publicKeys.getKeyByKeyId(keyId);

        String pubKeyString = "-----BEGIN PUBLIC KEY-----\n"
                + new String(new Base64().encode(((AsymmetricJWK) jwk).toPublicKey().getEncoded())) + "-----END PUBLIC KEY-----";
        properties.put(ApplicationInfo.KEY_PUBLIC_KEY, pubKeyString);
        AdminServiceFactory.getInstance().addApplication(properties);
    }


}
