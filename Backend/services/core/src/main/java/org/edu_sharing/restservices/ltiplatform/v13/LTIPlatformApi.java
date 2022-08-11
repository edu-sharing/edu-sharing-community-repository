package org.edu_sharing.restservices.ltiplatform.v13;

import io.jsonwebtoken.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.PersonService;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.tools.EduSharingNodeHelper;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.UrlTool;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.security.Signing;
import org.edu_sharing.restservices.RepositoryDao;
import org.edu_sharing.restservices.RestConstants;
import org.edu_sharing.restservices.lti.v13.ApiTool;
import org.edu_sharing.restservices.ltiplatform.v13.model.*;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.edu_sharing.service.lti13.LTIConstants;
import org.edu_sharing.service.lti13.LTIJWTUtil;
import org.edu_sharing.service.lti13.registration.RegistrationService;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.version.VersionService;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.extensions.surf.util.I18NUtil;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.util.*;

@Path("/ltiplatform/v13")
@Consumes({ "text/html" })
@Produces({"text/html"})
@Tag(name="LTI Platform v13")
public class LTIPlatformApi {

    Logger logger = Logger.getLogger(LTIPlatformApi.class);

    ServiceRegistry serviceRegistry = (ServiceRegistry)AlfAppContextGate.getApplicationContext().getBean(ServiceRegistry.SERVICE_REGISTRY);
    NodeService nodeService = serviceRegistry.getNodeService();
    PersonService personService = serviceRegistry.getPersonService();


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

            String username = AuthenticationUtil.getFullyAuthenticatedUser();

            if(!username.equals(loginHint)){
                throw new Exception("wrong login_hint. does not match session login");
            }

            LoginInitiationSessionObject loginInitiationSessionObject = (LoginInitiationSessionObject)req.getSession().getAttribute(LTIPlatformConstants.LOGIN_INITIATIONS_SESSIONOBJECT);

            if(loginInitiationSessionObject == null){
                throw new Exception("lti lti session object found");
            }

            if(!loginInitiationSessionObject.getParentId().equals(ltiMessageHint)){
                throw new Exception("wrong context:" + ltiMessageHint);
            }

            ApplicationInfo appInfo = ApplicationInfoList.getRepositoryInfoById(loginInitiationSessionObject.getAppId());
            if(appInfo == null){
                throw new Exception("invalid request: application");
            }

            if(!appInfo.getLtiClientId().equals(clientId)){
                throw new Exception("unauthorized_client");
            }

            if(!Arrays.asList(appInfo.getLtitoolRedirectUrls().split(",")).contains(redirect_uri)){
                throw new Exception("invalid request: redirect_url");
            }

            if(responseMode == null || !responseMode.equals("form_post")){
                throw new Exception("invalid request: response_mode");
            }

            ApplicationInfo homeApp = ApplicationInfoList.getHomeRepository();

            Map<String,Object> context = new HashMap<>();
            context.put("id", loginInitiationSessionObject.getParentId());
            context.put("label",nodeService
                    .getProperty(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, loginInitiationSessionObject.getParentId()), ContentModel.PROP_NAME));

            String firstName = (String)nodeService.getProperty(personService.getPerson(username),ContentModel.PROP_FIRSTNAME);
            String lastName = (String)nodeService.getProperty(personService.getPerson(username),ContentModel.PROP_LASTNAME);
            String email = (String)nodeService.getProperty(personService.getPerson(username),ContentModel.PROP_EMAIL);

            Map<String,Object> launchPresentation = new HashMap<>();
            launchPresentation.put("locale", I18NUtil.getLocale());



            Map<String,Object> toolPlatform = new HashMap<>();
            toolPlatform.put("product_family_code","edu-sharing");
            toolPlatform.put("version",VersionService.getVersion(VersionService.Type.REPOSITORY));
            toolPlatform.put("guid",homeApp.getAppId());
            toolPlatform.put("name",homeApp.getAppCaption());
            toolPlatform.put("description",homeApp.getAppCaption());

            Map<String,Object> deepLinkingSettings = new HashMap<>();
            deepLinkingSettings.put("accept_types",Arrays.asList(new String[]{"ltiResourceLink"}));
            deepLinkingSettings.put("accept_presentation_document_targets",Arrays.asList(new String[]{"iframe","window"}));
            deepLinkingSettings.put("accept_copy_advice",false);
            deepLinkingSettings.put("accept_multiple",true);
            deepLinkingSettings.put("accept_unsigned",false);
            deepLinkingSettings.put("auto_create",false);
            deepLinkingSettings.put("can_confirm",false);
            deepLinkingSettings.put("deep_link_return_url",homeApp.getClientBaseUrl()+"/rest/ltiplatform/v13/deeplinking-response/");
            deepLinkingSettings.put("title",homeApp.getAppCaption());


            Key platformPrivateKey = new Signing().getPemPrivateKey(homeApp.getPrivateKey(), CCConstants.SECURITY_KEY_ALGORITHM);

            Date now = new Date();
            String jwt = Jwts.builder()
                    .setHeaderParam(LTIConstants.TYP, LTIConstants.JWT)
                    .setHeaderParam(LTIConstants.KID, homeApp.getLtiKid())
                    .setHeaderParam(LTIConstants.ALG, LTIConstants.RS256)
                    .claim("nonce",nonce)
                    .setIssuer(RegistrationService.getLtiPlatformOpenIdConfiguration().getIssuer())
                    .setIssuedAt(now)
                    .setExpiration(new Date((now.getTime() + 1000)))
                    .setAudience(clientId)
                    .setSubject(username)
                    .claim(LTIConstants.LTI_DEPLOYMENT_ID,appInfo.getLtiDeploymentId())
                    .claim(LTIConstants.LTI_TARGET_LINK_URI,appInfo.getLtitoolTargetLinkUri())
                    .claim(LTIConstants.DEEP_LINK_CONTEXT,context)
                    .claim("given_name",firstName)
                    .claim("family_name",lastName)
                    .claim("email",email)
                    .claim(LTIConstants.LTI_LAUNCH_PRESENTATION,launchPresentation)
                    .claim(LTIConstants.LTI_TOOL_PLATFORM,toolPlatform)
                    .claim(LTIConstants.LTI_VERSION, LTIConstants.LTI_VERSION_3)
                    .claim(LTIConstants.LTI_MESSAGE_TYPE,LTIConstants.LTI_MESSAGE_TYPE_DEEP_LINKING)
                    .claim(LTIConstants.DEEP_LINKING_SETTINGS,deepLinkingSettings)
                    .claim("https://purl.imsglobal.org/spec/lti/claim/roles",new ArrayList<>())
                    .signWith(platformPrivateKey,SignatureAlgorithm.RS256)
                    .compact();

            /**
             * @TODO compare lti message hint with session value
             */


            /**
             * @TODO build id_token and send it to redirect_uri
             */

            HashMap<String,String> formParams = new HashMap<>();
            formParams.put("id_token",jwt);
            formParams.put("state",state);
            return  Response.ok(ApiTool.getHTML(redirect_uri,formParams)).build();

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
        OpenIdConfiguration oidconf = RegistrationService.getLtiPlatformOpenIdConfiguration();
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
            ors.setClient_name(appInfo.getAppCaption());
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
            logger.error(e.getMessage(),e);
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
                    registrationData.getToolDescription(),
                    registrationData.getClientName(),
                    registrationData.getTargetLinkUriDeepLink());
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
                    tool.setName(appInfo.getAppCaption());
                    tool.setLogo(appInfo.getLogo());
                    tool.setCreateOption(appInfo.hasLtiToolCreateOption());
                    tools.getTools().add(tool);

                }
            }
            return Response.ok(tools).build();
        }catch (Exception e){
            return ErrorResponse.createResponse(e);
        }
    }


    @GET
    @Path("/generateLoginInitiationForm")
    @Operation(summary = "generate a form used for Initiating Login from a Third Party")
    @Consumes({ "text/html"})
    @Produces({"text/html"})
    @ApiResponses(
            value = {
                    @ApiResponse(responseCode="200", description= RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = String.class)))
            })
    public Response generateLoginInitiationForm(@Parameter(description = "appId of the tool",required=true) @QueryParam("appId") String appId,
                                                @Parameter(description = "the folder id the lti node will be created in",required=true) @QueryParam("parentId") String parentId,
                                                @Context HttpServletRequest req){


        try {
            RepositoryDao repoDao = RepositoryDao.getHomeRepository();

            if ("-userhome-".equals(parentId)) {
                parentId = repoDao.getUserHome();
            }
            if ("-inbox-".equals(parentId)) {
                parentId =repoDao.getUserInbox();
            }
            if ("-saved_search-".equals(parentId)) {
                parentId = repoDao.getUserSavedSearch();
            }

            for(ApplicationInfo appInfo : ApplicationInfoList.getApplicationInfos().values()){
                if(appInfo.isLtiTool() && appInfo.getAppId().equals(appId)){
                    Map<String,String> params = new HashMap<>();
                    params.put("iss",ApplicationInfoList.getHomeRepository().getClientBaseUrl());
                    params.put("target_link_uri",appInfo.getLtitoolTargetLinkUri());
                    params.put("login_hint", AuthenticationUtil.getFullyAuthenticatedUser());
                    params.put("lti_message_hint",parentId);
                    params.put("client_id",appInfo.getLtiClientId());
                    params.put("lti_deployment_id",appInfo.getLtiDeploymentId());
                    String form = ApiTool.getHTML(appInfo.getLtitoolLoginInitiationsUrl(),params);

                    LoginInitiationSessionObject loginInitiationSessionObject = new LoginInitiationSessionObject();
                    loginInitiationSessionObject.setAppId(appInfo.getAppId());
                    loginInitiationSessionObject.setClientId(appInfo.getLtiClientId());


                    loginInitiationSessionObject.setParentId(parentId);
                    req.getSession().setAttribute(LTIPlatformConstants.LOGIN_INITIATIONS_SESSIONOBJECT, loginInitiationSessionObject);

                    return Response.status(Response.Status.OK).entity(form).build();
                }
            }
            throw new Exception("no lti tool found for "+ appId);
        }catch (Exception e){
             return ApiTool.processError(req,e,"");
        }
    }


    @POST
    @Path("/deeplinking-response")

    @Operation(summary = "receiving deeplink response messages.", description = "deeplink response")
    @Consumes({"application/x-www-form-urlencoded"})
    @ApiResponses(
            value = {
                    @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="409", description=RestConstants.HTTP_409, content = @Content(schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = String.class)))
            })

    public Response deepLinkingResponse(
            @Parameter(description = "JWT",required=true) @FormParam("JWT") String jwt,
            @Context HttpServletRequest req) {
        try{

            LoginInitiationSessionObject sessionObject = (LoginInitiationSessionObject)req.getSession().getAttribute(LTIPlatformConstants.LOGIN_INITIATIONS_SESSIONOBJECT);
            LTIJWTUtil jwtUtil = new LTIJWTUtil();
            //find out clientid/deploymentid
            ApplicationInfo appInfoTool = ApplicationInfoList.getRepositoryInfoById(sessionObject.getAppId());
            Jws<Claims> claims = jwtUtil.validateJWT(jwt,appInfoTool);

            /**
             * @ToDo more validation?
             */
            if(!appInfoTool.isLtiTool()){
                throw new Exception("application is no lti tool");
            }


            if(sessionObject == null){
                throw new Exception("missing login initiation session object");
            }


            List<Map<String,Object>> contentItems = (List<Map<String,Object>>)claims.getBody().get(LTIConstants.LTI_CONTENT_ITEMS);
            if(contentItems == null || contentItems.size() == 0){
                throw new Exception("missing lti content items");
            }

            String nodeId = sessionObject.getNodeId();
            if(appInfoTool.hasLtiToolCreateOption()){

                if(nodeId == null){
                    throw new Exception("id from initial created node required");
                }
                if(contentItems.size() > 1) throw new Exception("only one node can be handled for lti tool: " + appInfoTool.getAppId());
            }


            List<String> nodeIds = new ArrayList<>();
            for(Map<String,Object> contentItem : contentItems){
                HashMap<String, String[]> properties = new HashMap<>();
                String type = (String)contentItem.get("type");
                if(!LTIConstants.DEEP_LINK_LTIRESOURCELINK.equals(type)){
                    throw new Exception("unsupported lti type:"+type);
                }

                String url = (String)contentItem.get("url");
                if(url == null){
                    throw new Exception("missing resourcelink url");
                }

                String title = (String)contentItem.get("title");
                properties.put(CCConstants.CCM_PROP_LTITOOL_NODE_RESOURCELINK,new String[]{url});
                title = title != null ? title : url;
                properties.put(CCConstants.CM_NAME,new String[]{EduSharingNodeHelper.cleanupCmName(title)} );
                properties.put(CCConstants.LOM_PROP_GENERAL_TITLE,new String[]{title});

                if(contentItem.containsKey("icon")){
                    Map<String,Object> icon = (Map<String,Object>)contentItem.get("icon");
                    String iconUrl = (String)icon.get("url");
                    if(iconUrl != null){
                        properties.put(CCConstants.CCM_PROP_IO_THUMBNAILURL,new String[]{iconUrl});
                    }
                }

                org.edu_sharing.service.nodeservice.NodeService eduNodeService = NodeServiceFactory.getLocalService();
                if(nodeId == null){
                    nodeId = eduNodeService.createNode(sessionObject.getParentId(), CCConstants.CCM_TYPE_IO,properties);
                }
                if(eduNodeService.hasAspect("workspace","SpacesStore",nodeId,CCConstants.CCM_ASPECT_LTITOOL_NODE)){
                    eduNodeService.addAspect(nodeId,CCConstants.CCM_ASPECT_LTITOOL_NODE);
                }
                nodeIds.add(nodeId);
            }


            String nodeIdsJS = (nodeIds.size() > 1) ? StringUtils.join(nodeIds.toArray(),"','") : nodeIds.get(0);
            nodeIdsJS = "['"+nodeIdsJS+"']";

            String closeAndInformAngular =
                    "function callAngularFunction(nodeIds) {" +
                            "window.opener.angularComponentReference.zone.run((nodeIds ) => { window.opener.angularComponentReference.loadAngularFunction(nodeIds); });" +
                    "}"
                    + "window.onload = function() {\n" +
                        " callAngularFunction("+nodeIdsJS+ ");\n" +
                            "window.close();\n" +
                    "};";

            return Response.ok().entity(ApiTool.getHTML(null,null,"no js active. please close tab.",closeAndInformAngular)).build();
        } catch (Throwable e) {
            return ApiTool.processError(req,e,"");
        }
    }

}
