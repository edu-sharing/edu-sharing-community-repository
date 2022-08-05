package org.edu_sharing.restservices.ltiplatform.v13;

import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.UrlTool;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.security.Signing;
import org.edu_sharing.restservices.RestConstants;
import org.edu_sharing.restservices.lti.v13.ApiTool;
import org.edu_sharing.restservices.ltiplatform.v13.model.*;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.edu_sharing.service.lti13.LTIConstants;
import org.edu_sharing.service.lti13.LTIJWTUtil;
import org.edu_sharing.service.lti13.registration.RegistrationService;
import org.edu_sharing.service.version.VersionService;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.util.*;

@Path("/ltiplatform/v13")
@Consumes({ "text/html" })
@Produces({"text/html"})
@Tag(name="LTI Platform v13")
public class LTIPlatformApi {

    Logger logger = Logger.getLogger(LTIPlatformApi.class);



    @GET
    @Path("/auth")
    @Operation(summary = "LTI Platform oidc endpoint. responds to a login authentication request")
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

    public Response auth(
            @Parameter(description = "scope",required=true) @QueryParam("scope") String scope,
            @Parameter(description = "response_type",required=true) @QueryParam("response_type") String responseType,
            @Parameter(description = "optional parameter client_id specifies the client id for the authorization server that should be used to authorize the subsequent LTI message request",required=false) @QueryParam("client_id") String clientId,
            @Parameter(description = "login_hint",required=true) @QueryParam("login_hint") String loginHint,
            @Parameter(description = "state",required=true) @QueryParam("state") String state,
            @Parameter(description = "response_mode",required=true) @QueryParam("response_mode") String responseMode,
            @Parameter(description = "nonce",required=true) @QueryParam("nonce") String nonce,
            @Parameter(description = "prompt",required=true) @QueryParam("prompt") String prompt,
            @Parameter(description = "Similarly to the login_hint parameter, lti_message_hint value is opaque to the tool. If present in the login initiation request, the tool MUST include it back in the authentication request unaltered",required=false) @QueryParam("lti_message_hint") String ltiMessageHint,
            @Parameter(description = "redirect_uri",required=true) @QueryParam("redirect_uri") String redirect_uri,
            @Context HttpServletRequest req){

        try {
            if (isEmpty(scope)) throw new Exception("missing param scope");
            if (isEmpty(responseType)) throw new Exception("missing param response_type");
            if (isEmpty(loginHint)) throw new Exception("missing param login_hint");
            if (isEmpty(responseMode)) throw new Exception("missing param response_mode");
            if (isEmpty(nonce)) throw new Exception("missing param nonce");
            if (isEmpty(prompt)) throw new Exception("missing param prompt");
            if (isEmpty(redirect_uri)) throw new Exception("missing param redirect_uri");

            if(!scope.equals("openid")) throw new Exception("invalid scope " +scope);
            if(!responseType.equals("id_token")) throw new Exception("unsupported response_type "+responseType);
            if(!responseMode.equals("form_post")){throw new Exception("invalid response_mode " +responseMode);}

            /**
             * @TODO compare lti message hint with session value
             */


            /**
             * @TODO build id_token and send it to redirect_uri
             */

            return null;
        } catch(Throwable e){
            return ApiTool.processError(req,e,"LTI_PLATFORM_AUTH_ERROR");
        }
    }

    private boolean isEmpty( String value){
        if(value == null || value.trim().isEmpty()){
            return true;
        }
        return false;
    }


    @POST
    @Path("/start-dynamic-registration" )
    @Operation(summary = "starts lti dynamic registration.", description = "start dynmic registration")

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
    public Response startDynamicRegistration(@Parameter(description = "url",required=true) @FormParam("url") String url,
                                           @Context HttpServletRequest req){

        //generate client id to allow multiple tool deployments
        String clientId = RegistrationService.generateNewClientId();

        Calendar cal = Calendar.getInstance();
        Date now = cal.getTime();
        cal.add(Calendar.HOUR,1);
        Date exp = cal.getTime();

        ApplicationInfo homeApp = ApplicationInfoList.getHomeRepository();

        try {
            String registrationToken = Jwts.builder()
                    .setSubject(clientId)
                    .setIssuedAt(now)
                    .setExpiration(exp)
                    .claim("scope",LTIConstants.LTI_REGISTRATION_SCOPE_NEW)
                    .signWith(new Signing().getPemPrivateKey(homeApp.getPrivateKey(), CCConstants.SECURITY_KEY_ALGORITHM))
                    .compact();
            String openIdConfigurationUrl = homeApp.getClientBaseUrl()+"/rest/ltiplatform/v13/openid-configuration/";
            url = UrlTool.setParam(url,"openid_configuration", openIdConfigurationUrl);
            url = UrlTool.setParam(url,"registration_token", registrationToken);
            return Response.seeOther(new URI(url)).build();
        } catch (GeneralSecurityException | URISyntaxException e) {
            logger.error(e.getMessage(),e);
            return ErrorResponse.createResponse(e);
        }
    }


    @GET
    @Path("/openid-configuration")
    @Operation(summary = "LTIPlatform openid configuration")
    @Consumes({ "*/*"})
    @Produces({"application/json"})
    @ApiResponses(
            value = {
                    @ApiResponse(responseCode="200", description= RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = OpenIdConfiguration.class))),
                    @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = String.class)))
            })
    public Response openidConfiguration(){
        ApplicationInfo homeRepository = ApplicationInfoList.getHomeRepository();
        OpenIdConfiguration oidconf = new OpenIdConfiguration();
        oidconf.setIssuer(homeRepository.getDomain());
        /**
         * @TODO token stuff
         */
        //oidconf.setToken_endpoint();
        oidconf.setToken_endpoint_auth_methods_supported(Arrays.asList("private_key_jwt"));
        oidconf.setToken_endpoint_auth_signing_alg_values_supported(Arrays.asList(SignatureAlgorithm.RS256.getValue()));
        oidconf.setJwks_uri(homeRepository.getClientBaseUrl()+"/rest/lti/v13/jwks");
        oidconf.setAuthorization_endpoint(homeRepository.getClientBaseUrl()+"/rest/ltiplatform/v13/auth");
        oidconf.setRegistration_endpoint(homeRepository.getClientBaseUrl()+"/rest/ltiplatform/v13/openid-registration");
        oidconf.setToken_endpoint(homeRepository.getClientBaseUrl()+"/rest/ltiplatform/v13/token");
        oidconf.setResponse_types_supported(Arrays.asList("id_token"));
        oidconf.setClaims_supported(Arrays.asList("sub","iss","given_name","family_name","email"));

        OpenIdConfiguration.LTIPlatformConfiguration ltiPlatformConfiguration = new OpenIdConfiguration.LTIPlatformConfiguration();
        OpenIdConfiguration.LTIPlatformConfiguration.Message msgDeepLink = new OpenIdConfiguration.LTIPlatformConfiguration.Message();
        msgDeepLink.setType("LtiDeepLinkingRequest");
        OpenIdConfiguration.LTIPlatformConfiguration.Message msgResourceLink = new OpenIdConfiguration.LTIPlatformConfiguration.Message();
        msgResourceLink.setType("LtiResourceLinkRequest");
        ltiPlatformConfiguration.getMessages_supported().add(msgDeepLink);
        ltiPlatformConfiguration.getMessages_supported().add(msgResourceLink);
        ltiPlatformConfiguration.setProduct_family_code("edu-sharing");
        ltiPlatformConfiguration.setVersion(VersionService.getVersionNoException(VersionService.Type.REPOSITORY));
        oidconf.setLtiPlatformConfiguration(ltiPlatformConfiguration);


        return Response.status(Response.Status.OK).entity(oidconf).build();
    }


    @POST
    @Path("/openid-registration")

    @Operation(summary = "registration endpoint the tool uses to register at platform.", description = "tool registration")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    @ApiResponses(
            value = {
                    @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = OpenIdRegistrationResult.class))),
                    @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode="409", description=RestConstants.HTTP_409, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            })

    public Response openIdRegistration(
            @Parameter(description = "registrationpayload" ,required=true ) String registrationpayload,
            @Context HttpServletRequest req) {
        try{

            String authorizationHeader = req.getHeader("Authorization");
            if(authorizationHeader == null || !authorizationHeader.substring(0,7).equals("Bearer ")){
                throw new Exception("missing_registration_token");
            }

            logger.debug("registrationpayload:"+registrationpayload);
            logger.debug("authorizationHeader:" +authorizationHeader);

            String registrationToken = authorizationHeader.substring(7);

            //validate registration token originally send by us
            //should not be reusable cause of exp date
            Jwt jwt = LTIJWTUtil.validateJWT(registrationToken, ApplicationInfoList.getHomeRepository());

            JSONParser jsonParser = new JSONParser();
            JSONObject registrationPayload =  (JSONObject)jsonParser.parse(registrationpayload);
            ApplicationInfo appInfo = AuthenticationUtil.runAsSystem(() -> {
               return new RegistrationService().ltiDynamicToolRegistration(registrationPayload, jwt);
            });


            OpenIdRegistrationResult ors = new OpenIdRegistrationResult();
            ors.setClient_id(appInfo.getLtiClientId());
            ors.setApplication_type("web");
            ors.setClient_name(appInfo.getAppId());
            ors.setInitiate_login_uri(appInfo.getLtitoolLoginInitiationsUrl());
            ors.setRedirect_uris(Arrays.asList(appInfo.getLtitoolRedirectUrls().split(",")));
            ors.setToken_endpoint_auth_method((String)registrationPayload.get("token_endpoint_auth_method"));
            ors.setLogo_uri(appInfo.getLogo());
            ors.setScope((String)registrationPayload.get("scope"));

            OpenIdRegistrationResult.LTIToolConfiguration ltiToolConfiguration = new OpenIdRegistrationResult.LTIToolConfiguration();
            //ltiToolConfiguration.setVersion();
            ltiToolConfiguration.setDeployment_id(appInfo.getLtiDeploymentId());
            ltiToolConfiguration.setTarget_link_uri(appInfo.getLtitoolTargetLinkUri());
            ltiToolConfiguration.setDomain(appInfo.getDomain());
            //ltiToolConfiguration.setDescription();
            //ltiToolConfiguration.setClaims();
            ors.setLtiToolConfiguration(ltiToolConfiguration);
            return Response.ok().entity(ors).build();
        } catch (Exception e) {
            return ErrorResponse.createResponse(e);
        }
    }


    @GET
    @Path("/token")
    @Operation(summary = "LTIPlatform auth token endpoint")
    @Consumes({ "*/*"})
    @Produces({"application/json"})
    @ApiResponses(
            value = {
                    @ApiResponse(responseCode="200", description= RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = Void.class))),
                    @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = String.class)))
            })
    public Response authTokenEndpoint(){

        try {
            if (true) {
                throw new Exception("not implemented yet");
            }
        }catch (Exception e){
            return ErrorResponse.createResponse(e);
        }

        return Response.status(Response.Status.OK).build();
    }


    @POST
    @Path("/manual-registration")

    @Operation(summary = "manual registration endpoint for registration of tools.", description = "tool registration")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    @ApiResponses(
            value = {
                    @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = Void.class))),
                    @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode="409", description=RestConstants.HTTP_409, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            })

    public Response manualRegistration(
            @Parameter(description = "registrationData" ,required=true ) ManualRegistrationData registrationData,
            @Context HttpServletRequest req) {
        try {
            new RegistrationService().registerTool(
                    registrationData.getToolUrl(),
                    RegistrationService.generateNewClientId(),
                    registrationData.getLoginInitiationUrl(),
                    registrationData.getKeysetUrl(),
                    null,
                    StringUtils.join(registrationData.getRedirectionUrls(), ","),
                    registrationData.getLogoUrl(),
                    (registrationData.getCustomParameters() != null) ? StringUtils.join(registrationData.getCustomParameters(),",") : null,
                    registrationData.getToolDescription());
            return Response.ok().build();
        }catch (Exception e){
            return ErrorResponse.createResponse(e);
        }
    }

    @GET
    @Path("/tools")
    @Operation(summary = "List of tools registered")
    @Consumes({ "application/json"})
    @Produces({"application/json"})
    @ApiResponses(
            value = {
                    @ApiResponse(responseCode="200", description= RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = Tools.class))),
                    @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = String.class)))
            })
    public Response tools(){

        try {
            Tools tools = new Tools();
            for(ApplicationInfo appInfo : ApplicationInfoList.getApplicationInfos().values()){
                if(appInfo.isLtiTool()){
                    Tool tool = new Tool();
                    tool.setAppId(appInfo.getAppId());
                    tool.setDescription(appInfo.getLtitoolDescription());
                    try {
                        URI uri = new URI(appInfo.getLtitoolLoginInitiationsUrl());
                        tool.setDomain(uri.getHost());
                    }catch ( java.net.URISyntaxException e){}
                    tools.getTools().add(tool);
                }
            }
            return Response.ok(tools).build();
        }catch (Exception e){
            return ErrorResponse.createResponse(e);
        }
    }
}
