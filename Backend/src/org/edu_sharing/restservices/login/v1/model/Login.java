package org.edu_sharing.restservices.login.v1.model;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.edu_sharing.repository.server.authentication.LoginHelper;
import org.edu_sharing.repository.server.authentication.RemoteAuthDescription;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.service.lti13.model.LTISessionObject;
import org.edu_sharing.service.toolpermission.ToolPermissionServiceFactory;

import com.fasterxml.jackson.annotation.JsonProperty;


@Schema(description = "")
public class Login  {

  private Map<String, RemoteAuthDescription> remoteAuthentications;
  private boolean isValidLogin;
  private String currentScope;
  private String userHome;
  private int sessionTimeout;
  private boolean isGuest;
  private List<String> toolPermissions;
  private boolean isAdmin;
  private String statusCode;
  private String authorityName;
  private boolean isLtiSession;
  
  
  public final static String STATUS_CODE_OK ="OK";
  public final static String STATUS_CODE_GUEST ="GUEST";
  public final static String STATUS_CODE_INVALID_CREDENTIALS ="INVALID_CREDENTIALS";
  public final static String STATUS_CODE_PREVIOUS_SESSION_REQUIRED = "PREVIOUS_SESSION_REQUIRED";
  public final static String STATUS_CODE_PREVIOUS_USER_WRONG = "PREVIOUS_USER_WRONG";
  public final static String STATUS_CODE_INVALID_SCOPE = "INVALID_SCOPE";
  public final static String STATUS_CODE_PASSWORD_EXPIRED = "PASSWORD_EXPIRED";
  public final static String STATUS_CODE_PERSON_BLOCKED = "PERSON_BLOCKED";


  public Login(){

  }


public Login(boolean isValidLogin, String scope, HttpSession session) {
	  this(isValidLogin,scope,null,session, (isValidLogin) ? STATUS_CODE_OK : STATUS_CODE_INVALID_CREDENTIALS );
  }
  
  public Login(boolean isValidLogin, String scope, String userHome, HttpSession session, String statusCode) {
	
	org.edu_sharing.service.authority.AuthorityService service=AuthorityServiceFactory.getAuthorityService(ApplicationInfoList.getHomeRepository().getAppId());
	
	if(isValidLogin){
		try{
			this.toolPermissions=ToolPermissionServiceFactory.getInstance().getAllAvailableToolPermissions();
		}catch(Throwable t){
			// not logged in
		}
	}
	this.statusCode = service.isGuest() ? STATUS_CODE_GUEST : statusCode;
	this.authorityName=service.isGuest() ? null : AuthenticationUtil.getFullyAuthenticatedUser();
	this.isAdmin=service.isGlobalAdmin();
	this.isValidLogin = isValidLogin;
	this.currentScope = scope;
	if(isValidLogin && scope==null && !service.isGuest())
	    this.remoteAuthentications = LoginHelper.getRemoteAuthsForSession();
	this.userHome = userHome;
	this.isGuest = service.isGuest();
	this.sessionTimeout = session.getMaxInactiveInterval();

      LTISessionObject ltiSessionObject = (LTISessionObject)session.getAttribute(LTISessionObject.class.getName());
      if(ltiSessionObject != null){
          this.isLtiSession = true;
      }
  }
  	@JsonProperty("authorityName")
	public String getAuthorityName() {
		return authorityName;
	}
	
	public void setAuthorityName(String authorityName) {
		this.authorityName = authorityName;
	}

@Schema(required = true, description = "")
  @JsonProperty("isGuest")
  public boolean isGuest() {
		return isGuest;
	}

	public void setGuest(boolean isGuest) {
		this.isGuest = isGuest;
	}
  @Schema(required = true, description = "")
  @JsonProperty("sessionTimeout")
  public int getSessionTimeout() {
	return sessionTimeout;
}

public void setSessionTimeout(int sessionTimeout) {
	this.sessionTimeout = sessionTimeout;
}
/**
   **/
  @Schema(required = true, description = "")
  @JsonProperty("currentScope")
  public String getCurrentScope() {
    return currentScope;
  }
  public void setCurrentScope(String currentScope) {
    this.currentScope = currentScope;
  }
  @Schema(required = true, description = "")
  @JsonProperty("isValidLogin")
  public boolean isValidLogin() {
    return isValidLogin;
  }
  public void setIsValidLogin(boolean isValidLogin) {
    this.isValidLogin = isValidLogin;
  }
  @Schema(required = true, description = "")
  @JsonProperty("isAdmin")
  public boolean isAdmin() {
    return isAdmin;
  }
  public void setIsAdmin(boolean isAdmin) {
    this.isAdmin = isAdmin;
  }
  @Schema(required = false, description = "")
  @JsonProperty("userHome")
  public String getUserHome() {
	return userHome;
}
  @JsonProperty("toolPermissions")
  public List<String> getToolPermissions() {
	return toolPermissions;
  }
  @JsonProperty
  public Map<String, RemoteAuthDescription> getRemoteAuthentications() {
      return remoteAuthentications;
  }

  @JsonProperty("statusCode")
  public String getStatusCode() {
	return statusCode;
  }
    @JsonProperty("isLtiSession")
    public boolean isLtiSession() {
        return isLtiSession;
    }

    public void setToolPermissions(List<String> toolPermissions) {
	this.toolPermissions = toolPermissions;
}

@Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class Login {\n");
    
    sb.append("  isValidLogin: ").append(isValidLogin).append("\n");
    sb.append("  isAdmin: ").append(isAdmin).append("\n");
    sb.append("  currentScope: ").append(currentScope).append("\n");
    sb.append("  userHome: ").append(userHome).append("\n");
    sb.append("  statusCode: ").append(statusCode).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}
