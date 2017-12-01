package org.edu_sharing.restservices.shared;

import java.util.ArrayList;
import java.util.List;

import org.edu_sharing.repository.client.rpc.Group;
import org.edu_sharing.repository.client.rpc.User;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.restservices.GroupDao;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "")
public class ACE  {
	
	private Authority authority = null;
	private UserProfile userProfile = null;
	private GroupProfile groupProfile = null;	
	private List<String> permissions = null;	
	
	boolean isEditable = true;
	
	public ACE(Authority authority,UserProfile user,GroupProfile group) {
		setAuthority(authority);
		setUserProfile(user);
		setGroupProfile(group);
	}
	public ACE(){}
	public ACE(org.edu_sharing.repository.client.rpc.ACE ace) {
		authority=new Authority(ace.getAuthority(),ace.getAuthorityType());
		if(ace.getGroup()!=null)
			groupProfile=new GroupProfile(ace.getGroup());
		if(ace.getUser()!=null)
			userProfile=new UserProfile(ace.getUser());
			
		permissions=new ArrayList<>();
		permissions.add(ace.getPermission());		
	}
	
	@ApiModelProperty(required = true, value = "")
	@JsonProperty("authority")
	public Authority getAuthority() {
		return authority;
	}
	public void setAuthority(Authority authority) {
		this.authority = authority;
	}
	@ApiModelProperty(required = false, value = "")
	@JsonProperty("user")
	public UserProfile getUserProfile() {
		return userProfile;
	}
	public void setUserProfile(UserProfile user){
		this.userProfile=user;
	}
	@ApiModelProperty(required = false, value = "")
	@JsonProperty("group")
	public GroupProfile getGroupProfile() {
		return groupProfile;
	}
	public void setGroupProfile(GroupProfile group){
		this.groupProfile=group;
	}


	@ApiModelProperty(required = true, value = "")
	@JsonProperty("permissions")
	public List<String> getPermissions() {
		return permissions;
	}
	public void setPermissions(List<String> permissions) {
		this.permissions = permissions;
	}
	@JsonIgnore
	public void setUser(User user) {
		if(user==null)
			return;
		this.userProfile = new UserProfile();
		this.userProfile.setFirstName(user.getGivenName());
		this.userProfile.setLastName(user.getSurname());
		this.userProfile.setEmail(user.getEmail());
	}
	@JsonIgnore
	public void setGroup(Group group) {
		if(group==null)
			return;
		this.groupProfile=new GroupProfile();
		this.groupProfile.setDisplayName(group.getDisplayName());
	}
	
	public void setEditable(boolean isEditable) {
		this.isEditable = isEditable;
	}
	
	public boolean isEditable() {
		return isEditable;
	}
	
}
