package org.edu_sharing.restservices.lti.v13;

import com.google.gson.Gson;
import com.nimbusds.jose.jwk.*;
import edu.uoc.elc.lti.tool.Tool;
import edu.uoc.elc.lti.tool.oidc.LoginRequest;
import edu.uoc.elc.spring.lti.security.openid.HttpSessionOIDCLaunchSession;
import edu.uoc.elc.spring.lti.security.openid.LoginRequestFactory;
import io.jsonwebtoken.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.UrlTool;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.HttpQueryTool;
import org.edu_sharing.repository.server.tools.security.Signing;
import org.edu_sharing.restservices.NodeDao;
import org.edu_sharing.restservices.RepositoryDao;
import org.edu_sharing.restservices.RestConstants;
import org.edu_sharing.restservices.lti.v13.model.JWKResult;
import org.edu_sharing.restservices.lti.v13.model.JWKSResult;
import org.edu_sharing.restservices.lti.v13.model.RegistrationUrl;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.edu_sharing.restservices.shared.Node;
import org.edu_sharing.restservices.shared.NodeLTIDeepLink;
import org.edu_sharing.service.admin.AdminServiceFactory;
import org.edu_sharing.service.authority.AuthorityServiceHelper;
import org.edu_sharing.service.lti13.*;
import org.edu_sharing.service.lti13.model.LTISessionObject;
import org.edu_sharing.service.lti13.uoc.Config;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URL;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.*;

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

            /**
             * fix: when it's an LtiResourceLinkRequest moodle sends rendering url (/edu-sharing/components/render)
             * as targetUrl. edu.uoc.elc.lti.tool.Tool take this url for redirect_url which is wrong
             */
            authRequest = UrlTool.removeParam(authRequest,"redirect_uri");
            authRequest = UrlTool.setParam(authRequest,"redirect_uri",ApplicationInfoList.getHomeRepository().getClientBaseUrl()+"/rest/lti/v13/" + LTIConstants.LTI_TOOL_REDIRECTURL_PATH);


            //response.sendRedirect(authRequest);
            return Response.status(302).location(new URI(authRequest)).build();

        } catch (Throwable e) {
            logger.error(e.getMessage(),e);
            return Response.status(Response.Status.OK).entity(getHTML(null,null,"error:" + e.getMessage())).build();
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

    private String getHTML(String formTargetUrl, Map<String,String> params, String errorMessage){
        return this.getHTML(formTargetUrl,params,errorMessage,null);
    }
    /**
     * @TODO use template engine?
     * @param formTargetUrl
     * @param params
     * @return
     */
    private String getHTML(String formTargetUrl, Map<String,String> params, String message, String javascript){
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body>");
        if(javascript != null){
            sb.append("<script type=\"text/javascript\">"+javascript+"</script>");
        }
        if(message == null) {
            String FORMNAME = "ltiform";
            sb.append("<script type=\"text/javascript\">window.onload=function(){document.forms[\""+FORMNAME+"\"].submit();}</script>");
            sb.append("<form action=\"" + formTargetUrl + "\" method=\"post\" name=\"" + FORMNAME + "\"");
            for (Map.Entry<String, String> entry : params.entrySet()) {
                sb.append("<input type=\"hidden\" id=\"" + entry.getKey() + "\" name=\"" + entry.getKey() + "\" value=\"" + entry.getValue() + "\" class=\"form-control\"/>");
            }
            sb.append("<input type=\"submit\" value=\"Submit POST\" class=\"btn btn-primary\">")
                    .append("</form>");
        }else {
            sb.append(message);
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

            Tool tool = Config.getTool(ltijwtUtil.getPlatform(),req,false);

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
                        return Response.status(Response.Status.OK).entity(getHTML(null,null,"error: " + message)).build();
                    }
                }
            }
        }catch(Exception e){
            logger.error(e.getMessage(),e);
            return Response.status(Response.Status.OK).entity(getHTML(null,null,"error: "+ e.getMessage())).build();
        }


        return Response.status(Response.Status.OK).build();
    }

    @GET
    @Path("/generateDeepLinkingResponse")
    @Operation(summary = "generate DeepLinkingResponse")
    @Consumes({ "application/json" })
    @Produces({ "application/json"})
    @ApiResponses(value = {
            @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = NodeLTIDeepLink.class))),
            @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })
    public Response generateDeepLinkingResponse(@Parameter(description = "selected node id's",required=true)  @QueryParam("nodeIds")  List<String> nodeIds,
                                                @Context HttpServletRequest req){
        LTISessionObject ltiSessionObject = (LTISessionObject)req
                .getSession()
                .getAttribute(LTISessionObject.class.getName());
        try {
            if (ltiSessionObject != null) {
                RepositoryDao repoDao = RepositoryDao.getHomeRepository();
                List<Node> nodes = new ArrayList<>();
                for(String nodeId : nodeIds){
                    Node n = NodeDao.getNode(repoDao, nodeId).asNode();
                    nodes.add(n);
                }

                NodeLTIDeepLink dl = new NodeLTIDeepLink((String)ltiSessionObject.getDeepLinkingSettings().get(LTIConstants.DEEP_LINK_RETURN_URL),
                        new LTIJWTUtil().getDeepLinkingResponseJwt(ltiSessionObject, nodes.toArray(new Node[]{})));
                return Response.ok(dl).build();
            }else{
                throw new Exception("no active lti session");
            }
        }catch (Throwable t){
            return ErrorResponse.createResponse(t);
        }
    }

    /**
     * jsonResponse.put("jwks_uri",homeApp.getClientBaseUrl()+"/rest/lti/v13/jwks");
     *
     */
    @GET
    @Path("/jwks")
    @Operation(summary = "LTI - returns repository JSON Web Key Sets")
    @Consumes({ "application/json" })
    @Produces({"application/json"})
    @ApiResponses(
            value = {
                    @ApiResponse(responseCode="200", description= RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = RegistrationUrl.class))),
                    @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = String.class)))
            })
    public Response jwksUri(){
        try {

            ApplicationInfo homeApp = ApplicationInfoList.getHomeRepository();
            Signing signing = new Signing();
            PublicKey pemPublicKey = signing.getPemPublicKey(homeApp.getPublicKey(), CCConstants.SECURITY_KEY_ALGORITHM);
            RSAPublicKey pub = (RSAPublicKey)pemPublicKey;
            JWKSResult rs = new JWKSResult();
            JWKResult JWKResult = new Gson().fromJson(new RSAKey.Builder(pub)
                    .keyUse(KeyUse.SIGNATURE)
                    //.privateKey((RSAPrivateKey)privKey)
                    //.keyID(UUID.randomUUID().toString())
                    /**
                     * @TODO allow more keys, ie for every deployment an own
                     */
                    .keyID("1")
                    .build().toPublicJWK().toJSONString(), JWKResult.class);
            JWKResult.setAlg(SignatureAlgorithm.RS256.getValue());

            rs.setKeys(Arrays.asList(new JWKResult[]{JWKResult}));
            return Response.status(Response.Status.OK).entity(rs).build();
        }catch(Throwable e){
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse(e)).build();
        }
    }

    @GET
    @Path("/registration/initiate/{token}")
    @Operation(summary = "LTI Dynamic Registration - Initiate registration")
    @Consumes({ "text/html" })
    @Produces({"text/html"})
    @ApiResponses(
            value = {
                    @ApiResponse(responseCode="200", description= RestConstants.HTTP_200, content = @Content(mediaType = "text/html", schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(mediaType = "text/html", schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(mediaType = "text/html", schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(mediaType = "text/html", schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(mediaType = "text/html", schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(mediaType = "text/html", schema = @Schema(implementation = String.class)))
            })
    public Response ltiRegistrationInit(@Parameter(description = "the endpoint to the open id configuration to be used for this registration",required=true) @QueryParam("openid_configuration") String openidConfiguration,
                                        @Parameter(description = "the registration access token. If present, it must be used as the access token by the tool when making the registration request to the registration endpoint exposed in the openid configuration.",required=false) @QueryParam("registration_token") String registrationToken,
                                        @Parameter(description = "one time usage token which is autogenerated with the url in edu-sharing admin gui.", required = true) @PathParam("token") String eduSharingRegistrationToken,
                                        @Context HttpServletRequest req){

        try {
            System.out.println("openidConfiguration:" + openidConfiguration);
            System.out.println("registrationToken:" + registrationToken);
            System.out.println("edu-sharing token:" + eduSharingRegistrationToken);

            if(eduSharingRegistrationToken == null || eduSharingRegistrationToken.trim().equals("")){
                throw new Exception("no eduSharingRegistrationToken provided");
            }
            HttpSession session = req.getSession();
            if (session == null) {
                throw new Exception("no session found");
            }

            String ltiRegistrationToken = (String)session.getAttribute(LTIConstants.LTI_EDU_SHARING_REGISTRATION_TOKEN);
            if(ltiRegistrationToken == null){
                throw new Exception("no eduSharingRegistrationToken found in session");
            }

            if(!eduSharingRegistrationToken.equals(ltiRegistrationToken)){
                session.removeAttribute("lti_registration_token");
                throw new Exception("eduSharingRegistrationToken provided is invalid");
            }


            String platformConfiguration = new HttpQueryTool().query(openidConfiguration);
            JSONParser jsonParser = new JSONParser();
            JSONObject oidConfig = (JSONObject) jsonParser.parse(platformConfiguration);
            String issuer = (String) oidConfig.get("issuer");
            /**
             * @TODO it seems that moodle can not be validated like spec
             * validate https://www.imsglobal.org/spec/lti-dr/v1p0#issuer-and-openid-configuration-url-match
             *
             * 3.5.1 Issuer and OpenID Configuration URL Match
             */
            String keySetUrl = (String) oidConfig.get("jwks_uri");
            if(keySetUrl == null){
                throw new Exception("no jwks_uri provided");
            }
            String authorizationEndpoint = (String) oidConfig.get("authorization_endpoint");
            if(authorizationEndpoint == null){
                throw new Exception("no authorization_endpoint provided");
            }
            String registrationEndpoint = (String) oidConfig.get("registration_endpoint");
            if(registrationEndpoint == null){
                throw new Exception("no registration_endpoint provided");
            }

            String authTokenUrl = (String) oidConfig.get("token_endpoint");
            if(authTokenUrl == null){
                throw new Exception("no token_endpoint provided");
            }

            ApplicationInfo homeApp = ApplicationInfoList.getHomeRepository();

            JSONObject jsonResponse = new JSONObject();
            jsonResponse.put("application_type","web");
            JSONArray respTypes = new JSONArray();
            respTypes.add("id_token");
            jsonResponse.put("response_types", respTypes);
            jsonResponse.put("initiate_login_uri",homeApp.getClientBaseUrl()+"/rest/lti/v13/oidc/login_initiations");
            String redirectUrl = homeApp.getClientBaseUrl()+"/rest/lti/v13/lti13";
            JSONArray ja = new JSONArray();
            ja.add(redirectUrl);
            jsonResponse.put("redirect_uris",ja);
            jsonResponse.put("client_name",homeApp.getAppCaption());
            jsonResponse.put("jwks_uri",homeApp.getClientBaseUrl()+"/rest/lti/v13/jwks");
            jsonResponse.put("logo_uri",homeApp.getLogo());
            jsonResponse.put("token_endpoint_auth_method", "private_key_jwt");
            JSONObject ltiDeepLink = new JSONObject();
            ltiDeepLink.put("type","LtiDeepLinkingRequest");
            ltiDeepLink.put("target_link_uri",redirectUrl);
            ltiDeepLink.put("label","add an edu-sharing content object");
            ltiDeepLink.put("label#de","Ein edu-sharing Inhalt hinzufuegen");
            JSONObject toolConfig = new JSONObject();
            toolConfig.put("domain",homeApp.getDomain());
            toolConfig.put("messages",ltiDeepLink);
            jsonResponse.put("https://purl.imsglobal.org/spec/lti-tool-configuration",toolConfig);
            //jsonResponse.put("token_endpoint_auth_method","private_key_jwt");
            HttpPost post = new HttpPost();
            post.setEntity(new StringEntity(jsonResponse.toJSONString()));
            post.setURI(new URI(registrationEndpoint));
            post.setHeader("Content-Type","application/json");
            post.setHeader("Accept","application/json");

            if(registrationToken != null && !registrationToken.trim().equals("")){
                post.setHeader("Authorization","Bearer "+registrationToken);
            }

            String result = new HttpQueryTool().query(null,null,post,false);

            System.out.println("registrationresult:"+result);
            JSONObject registrationResult;
            try {
                registrationResult = (JSONObject) jsonParser.parse(result);
            }catch(ParseException e){
                /**
                 * filter non json i.i when moodle notices and warnings are enabled
                 *
                 * <b>Warning</b>:  Illegal string offset 'type' in <b>/var/www/html/moodle/mod/lti/classes/local/ltiopenid/registration_helper.php</b> on line <b>172</b><br />
                 * <br />
                 * <b>Warning</b>:  Illegal string offset 'type' in <b>/var/www/html/moodle/mod/lti/classes/local/ltiopenid/registration_helper.php</b> on line <b>172</b><br />
                 * <br />
                 * <b>Warning</b>:  Illegal string offset 'type' in <b>/var/www/html/moodle/mod/lti/classes/local/ltiopenid/registration_helper.php</b> on line <b>172</b><br />
                 * <br />
                 * <b>Warning</b>:  Illegal string offset 'type' in <b>/var/www/html/moodle/mod/lti/classes/local/ltiopenid/registration_helper.php</b> on line <b>172</b><br />
                 * <br />
                 * <b>Notice</b>:  Undefined property: stdClass::$id in <b>/var/www/html/moodle/mod/lti/openid-registration.php</b> on line <b>56</b><br />
                 *
                 */

                int start=result.indexOf('{');
                int end=result.lastIndexOf('}');
                String json=result.substring(start,end+1);
                registrationResult = (JSONObject) jsonParser.parse(json);
                logger.warn("registration result could only be parsed after html cleanup. maybe disable warnings and notices on platform side.");
            }
            String clientId = (String)registrationResult.get("client_id");
            /**
             * {"client_id":"IcOCHxHupFSZz2Z","response_types":["id_token"],"jwks_uri":"https:\/\/localhost.localdomain\/edu-sharing\/rest\/lti\/v13\/jwks",
             * "initiate_login_uri":"https:\/\/localhost.localdomain\/edu-sharing\/rest\/lti\/v13\/oidc\/login_initiations",
             * "grant_types":["client_credentials","implicit"],"redirect_uris":["https:\/\/localhost.localdomain\/edu-sharing\/rest\/lti\/v13\/lti13"],
             * "application_type":"web","token_endpoint_auth_method":"private_key_jwt","client_name":"local",
             * "logo_uri":"\/edu-sharing\/images\/logos\/edu_sharing_com_login.svg","scope":"",
             * "https:\/\/purl.imsglobal.org\/spec\/lti-tool-configuration":
             *  {"version":"1.3.0","deployment_id":"5","target_link_uri":"https:\/\/localhost.localdomain",
             * "domain":"localhost.localdomain","description":"","claims":["sub","iss"]}}
             */
            JSONObject ltiToolConfigInfo = (JSONObject)registrationResult.get("https://purl.imsglobal.org/spec/lti-tool-configuration");
            String deploymentId = (String)ltiToolConfigInfo.get("deployment_id");

            registerPlatform(issuer, clientId, deploymentId, authorizationEndpoint, keySetUrl,null,authTokenUrl);
            return Response.status(Response.Status.OK).entity(getHTML(
                    null,
                    null,
                    "platform registered<br><button onClick=\"(window.opener || window.parent).postMessage({subject:'org.imsglobal.lti.close'}, '*');\">OK</button>",
                    null))
                    .build();
        }catch(Throwable e){
            logger.error(e.getMessage(),e);
            return Response.status(Response.Status.OK).entity(getHTML(null,null,"error:" + e.getMessage())).build();
        }finally {
            if(req.getSession() != null) req.getSession().removeAttribute(LTIConstants.LTI_EDU_SHARING_REGISTRATION_TOKEN);
        }

    }

    @GET
    @Path("/registration/url")
    @Operation(summary = "LTI Dynamic Registration - generates url for platform")
    @Consumes({ "application/json" })
    @Produces({"application/json"})
    @ApiResponses(
            value = {
                    @ApiResponse(responseCode="200", description= RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = RegistrationUrl.class))),
                    @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = String.class)))
            })
    public Response ltiRegistrationUrl(@Context HttpServletRequest req){

        try {
            String token = UUID.randomUUID().toString();
            req.getSession().setAttribute(LTIConstants.LTI_EDU_SHARING_REGISTRATION_TOKEN, token);
            RegistrationUrl result = new RegistrationUrl();
            result.setUrl(ApplicationInfoList.getHomeRepository().getClientBaseUrl()+"/rest/lti/v13/registration/initiate/"+token);
            return Response.status(Response.Status.OK).entity(result).build();
        }catch(Throwable e){
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse(e)).build();
        }
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
