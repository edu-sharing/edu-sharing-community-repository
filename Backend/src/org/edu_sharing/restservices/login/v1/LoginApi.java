package org.edu_sharing.restservices.login.v1;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.alfresco.service.cmr.repository.NodeRef;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.repository.server.authentication.LoginHelper;
import org.edu_sharing.repository.server.tools.Edu_SharingProperties;
import org.edu_sharing.repository.server.tools.security.ShibbolethSessions;
import org.edu_sharing.repository.server.tools.security.ShibbolethSessions.SessionInfo;
import org.edu_sharing.restservices.ApiService;
import org.edu_sharing.restservices.RestConstants;
import org.edu_sharing.restservices.login.v1.model.Login;
import org.edu_sharing.restservices.login.v1.model.LoginCredentials;
import org.edu_sharing.restservices.login.v1.model.ScopeAccess;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.edu_sharing.service.authentication.ScopeAuthenticationService;
import org.edu_sharing.service.authentication.ScopeAuthenticationServiceFactory;
import org.edu_sharing.service.authentication.ScopeUserHomeServiceFactory;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Path("/authentication/v1")
@Api(tags = {"AUTHENTICATION v1"})
@ApiService(value="AUTHENTICATION", major=1, minor=0)
public class LoginApi  {


	@GET       
	@Path("/validateSession")
    @ApiOperation(
    	value = "Validates the Basic Auth Credentials and check if the session is a logged in user", 
    	notes = "Use the Basic auth header field to transfer the credentials")
    
    @ApiResponses(
    	value = { 
    		@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Login.class),        
    	})

    public Response login(@Context HttpServletRequest req) {
		
		
    	AuthenticationToolAPI authTool = new AuthenticationToolAPI();
    	boolean authenticated = (authTool.validateAuthentication(req.getSession()) == null) ? false : true;
    	String personActiveStatus = Edu_SharingProperties.instance.getPersonActiveStatus();
		if(authenticated && personActiveStatus != null && !personActiveStatus.trim().equals("")) {
			String username = (String)req.getSession().getAttribute(CCConstants.AUTH_USERNAME);
			NodeRef authorityNodeRef = AuthorityServiceFactory.getLocalService().getAuthorityNodeRef(username);
			
			String personStatus = NodeServiceFactory.getLocalService().getProperty(authorityNodeRef.getStoreRef().getProtocol(), 
					authorityNodeRef.getStoreRef().getIdentifier(), 
					authorityNodeRef.getId(), CCConstants.CM_PROP_PERSON_ESPERSONSTATUS);
			if(!personActiveStatus.equals(personStatus)) {
				authenticated = false;
				authTool.logoutWithoutSecurityContext(authTool.getTicketFromSession(req.getSession()));
				req.getSession().invalidate();
			}
		}
    	return Response.ok(new Login(authenticated,authTool.getScope(),req.getSession())).build();
    }
    
    
    @POST      
   	@Path("/loginToScope")
       @ApiOperation(
       	value = "Validates the Basic Auth Credentials and check if the session is a logged in user", 
       	notes = "Use the Basic auth header field to transfer the credentials")
       
       @ApiResponses(
       	value = { 
       		@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Login.class),        
       	})

       public Response loginToScope(@ApiParam(value = "credentials, example: test,test" , required=true ) LoginCredentials credentials,
    		   @Context HttpServletRequest req) {
       		AuthenticationToolAPI authTool = new AuthenticationToolAPI();
       		ScopeAuthenticationService service = ScopeAuthenticationServiceFactory.getScopeAuthenticationService();
       	
       		HashMap<String,String> auth = authTool.validateAuthentication(req.getSession());
       		if(auth == null){
       			return Response.ok(new Login(false,null,  null,req.getSession(),Login.STATUS_CODE_PREVIOUS_SESSION_REQUIRED)).build();
       		}
       		
       		if(!credentials.getUserName().equals(auth.get(CCConstants.AUTH_USERNAME))){
       			return Response.ok(new Login(false,null,  null,req.getSession(),Login.STATUS_CODE_PREVIOUS_USER_WRONG)).build();
       		}
       		
       		/**
       		 * String shibbolethSessionId = getShibValue("Shib-Session-ID", req);
			
			if(shibbolethSessionId != null && !shibbolethSessionId.trim().equals("")){
				ShibbolethSessions.put(shibbolethSessionId, new SessionInfo(ticket, req.getSession()));
				req.getSession().setAttribute(CCConstants.AUTH_SSO_SESSIONID, shibbolethSessionId);
			}
       		 */
       		
       		/**
       		 * remember shibboleth session id to kill safe scope session by LogoutNotiFication 
       		 * 
       		 */
       		String shibbolethSessionId = (String)req.getSession().getAttribute(CCConstants.AUTH_SSO_SESSIONID);
       		
       		
       		boolean authenticated = service.authenticate(
       					credentials.getUserName(), 
       					credentials.getPassword(), 
       					credentials.getScope());
       		
       		String userHome = null;
       		
       		String statusCode = Login.STATUS_CODE_OK;
       		if(authenticated){
       			
       			NodeRef ref  = ScopeUserHomeServiceFactory.getScopeUserHomeService().getUserHome(credentials.getUserName(), credentials.getScope(), true);
       			userHome = ref.getId();
               	req.getSession().setMaxInactiveInterval(service.getSessionTimeout());
               	
               	if(shibbolethSessionId != null && !shibbolethSessionId.trim().equals("")){
    				ShibbolethSessions.put(shibbolethSessionId, new SessionInfo((String)req.getSession().getAttribute(CCConstants.AUTH_TICKET), req.getSession()));
    				req.getSession().setAttribute(CCConstants.AUTH_SSO_SESSIONID, shibbolethSessionId);
    			}
               	
       		}else{
       			statusCode = Login.STATUS_CODE_INVALID_CREDENTIALS;
       		}
       		return Response.ok(new Login(authenticated,authTool.getScope(), userHome,req.getSession(),statusCode)).build();
       }
    
    @GET       
   	@Path("/hasAccessToScope")
       @ApiOperation(
       	value = "Returns true if the current user has access to the given scope", 
       	notes = "")
       
       @ApiResponses(
       	value = { 
           		@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Void.class),        
           		@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = Void.class),        
       	})

       public Response hasAccessToScope(
    		   @ApiParam(value = "scope" , required=true ) @QueryParam("scope") String scope,
    		   @Context HttpServletRequest req) {
    	try{
       		AuthenticationToolAPI authTool = new AuthenticationToolAPI();
       		ScopeAuthenticationService service = ScopeAuthenticationServiceFactory.getScopeAuthenticationService();
       		boolean access=service.checkScope(authTool.getCurrentUser(), scope);
    		return Response.ok(new ScopeAccess(access)).build();
    	}catch(Throwable t){
    		return ErrorResponse.createResponse(t);
    	}
       }
    @GET       
   	@Path("/destroySession")
       @ApiOperation(
       	value = "Destroys the current session and logout the user", 
       	notes = "")
       
       @ApiResponses(
       	value = { 
           		@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Void.class),        
           		@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = Void.class),        
       	})

       public Response logout(@Context HttpServletRequest req) {
    	try{
    		new AuthenticationToolAPI().logout((String)req.getSession().getAttribute(CCConstants.AUTH_TICKET));
    		req.getSession().invalidate();
    		return Response.ok().build();
    	}catch(Throwable t){
    		// this may fail when the user is not logged in, so he is logged out anyway 
    		//return ErrorResponse.createResponse(t);
    		return Response.ok().build();
    	}
       }
       
    @OPTIONS        
    @ApiOperation(hidden = true, value = "")
    public Response options() {
    	
    	return Response.status(Response.Status.OK).header("Allow", "OPTIONS, GET").build();
    }
}

