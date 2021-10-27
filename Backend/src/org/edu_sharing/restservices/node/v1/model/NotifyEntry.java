package org.edu_sharing.restservices.node.v1.model;

import org.edu_sharing.repository.client.rpc.Notify;
import org.edu_sharing.restservices.shared.ACL;
import org.edu_sharing.restservices.shared.User;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;;

import com.fasterxml.jackson.annotation.JsonProperty;

@Schema(description = "")
public class NotifyEntry  {
  
	  private long date;
	  private ACL permissions;
	  private User user;
	  private String action;
	  public NotifyEntry() {}
	  public NotifyEntry(Notify notify) {
		  date=notify.getCreated().getTime();
		  action=notify.getNotifyAction();
		  user=new User(notify.getUser());
		  permissions=new ACL(notify.getAcl());
	  }
	@Schema(required = true, description = "")
	  @JsonProperty("date")
  public long getDate() {
		return date;
	}
	public void setDate(long date) {
		this.date = date;
	}
	  @Schema(required = true, description = "")
	  @JsonProperty("permissions")
	public ACL getPermissions() {
		return permissions;
	}
	public void setPermissions(ACL permissions) {
		this.permissions = permissions;
	}
	  @Schema(required = true, description = "")
	  @JsonProperty("user")
	public User getUser() {
		return user;
	}
	public void setUser(User user) {
		this.user = user;
	}
	  @Schema(required = true, description = "")
	  @JsonProperty("action")
	public String getAction() {
		return action;
	}
	public void setAction(String action) {
		this.action = action;
	}
}
