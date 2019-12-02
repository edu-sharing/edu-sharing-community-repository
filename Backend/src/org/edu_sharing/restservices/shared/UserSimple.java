package org.edu_sharing.restservices.shared;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.restservices.UserStatus;

public class UserSimple extends Authority{
	private String userName;
	private UserProfile profile = null;
	private UserStats stats = null;
    private UserStatus status;

    public UserSimple(){super();}
	public UserSimple(org.edu_sharing.repository.client.rpc.User user) {
		super(user.getAuthorityName(),user.getAuthorityType());
		userName=user.getAuthorityDisplayName();
		profile=new UserProfile(user);
	}

	public static UserSimple getDeletedUser(){
		UserSimple userSimple = new UserSimple();
		userSimple.setAuthorityName(CCConstants.AUTHORITY_DELETED_USER);
		userSimple.setAuthorityType(Type.USER);
		return userSimple;
	}

	/**
	 **/
	@ApiModelProperty(value = "")
	@JsonProperty("userName")
	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	/**
	 **/
	@ApiModelProperty(value = "")
	@JsonProperty("profile")
	public UserProfile getProfile() {
		return profile;
	}

	public void setProfile(UserProfile profile) {
		this.profile = profile;
	}
	
	@JsonProperty("stats")
	public UserStats getStats() {
		return stats;
	}
	public void setStats(UserStats stats) {
		this.stats = stats;
	}


    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public UserStatus getStatus() {
        return status;
    }
}
