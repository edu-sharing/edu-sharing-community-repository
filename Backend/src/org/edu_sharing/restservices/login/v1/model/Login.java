package org.edu_sharing.restservices.login.v1.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.edu_sharing.repository.server.authentication.LoginHelper;
import org.edu_sharing.repository.server.authentication.RemoteAuthDescription;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.service.toolpermission.ToolPermissionServiceFactory;

import com.fasterxml.jackson.annotation.JsonProperty;


@ApiModel(description = "")
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
  }
  	@JsonProperty("authorityName")
	public String getAuthorityName() {
		return authorityName;
	}
	
	public void setAuthorityName(String authorityName) {
		this.authorityName = authorityName;
	}

@ApiModelProperty(required = true, value = "")
  @JsonProperty("isGuest")
  public boolean isGuest() {
		return isGuest;
	}

	public void setGuest(boolean isGuest) {
		this.isGuest = isGuest;
	}
  @ApiModelProperty(required = true, value = "")
  @JsonProperty("sessionTimeout")
  public int getSessionTimeout() {
	return sessionTimeout;
}

public void setSessionTimeout(int sessionTimeout) {
	this.sessionTimeout = sessionTimeout;
}
/**
   **/
  @ApiModelProperty(required = true, value = "")
  @JsonProperty("currentScope")
  public String getCurrentScope() {
    return currentScope;
  }
  public void setCurrentScope(String currentScope) {
    this.currentScope = currentScope;
  }
  @ApiModelProperty(required = true, value = "")
  @JsonProperty("isValidLogin")
  public boolean isValidLogin() {
    return isValidLogin;
  }
  public void setIsValidLogin(boolean isValidLogin) {
    this.isValidLogin = isValidLogin;
  }
  @ApiModelProperty(required = true, value = "")
  @JsonProperty("isAdmin")
  public boolean isAdmin() {
    return isAdmin;
  }
  public void setIsAdmin(boolean isAdmin) {
    this.isAdmin = isAdmin;
  }
  @ApiModelProperty(required = false, value = "")
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
