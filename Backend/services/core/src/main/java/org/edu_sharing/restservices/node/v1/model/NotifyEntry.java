package org.edu_sharing.restservices.node.v1.model;

import org.edu_sharing.repository.client.rpc.Notify;
import org.edu_sharing.restservices.shared.ACL;
import org.edu_sharing.restservices.shared.User;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import com.fasterxml.jackson.annotation.JsonProperty;

@ApiModel(description = "")
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
	@ApiModelProperty(required = true, value = "")
	  @JsonProperty("date")
  public long getDate() {
		return date;
	}
	public void setDate(long date) {
		this.date = date;
	}
	  @ApiModelProperty(required = true, value = "")
	  @JsonProperty("permissions")
	public ACL getPermissions() {
		return permissions;
	}
	public void setPermissions(ACL permissions) {
		this.permissions = permissions;
	}
	  @ApiModelProperty(required = true, value = "")
	  @JsonProperty("user")
	public User getUser() {
		return user;
	}
	public void setUser(User user) {
		this.user = user;
	}
	  @ApiModelProperty(required = true, value = "")
	  @JsonProperty("action")
	public String getAction() {
		return action;
	}
	public void setAction(String action) {
		this.action = action;
	}
}
