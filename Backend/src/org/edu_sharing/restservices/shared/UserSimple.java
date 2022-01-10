package org.edu_sharing.restservices.shared;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.restservices.UserStatus;

import java.util.List;

public class UserSimple extends Authority{
	private String userName;
	private UserProfile profile = null;
    private UserStatus status;
	private List<Organization> organizations;

	public UserSimple(){super();}
	public UserSimple(org.edu_sharing.repository.client.rpc.User user) {
		super(user.getAuthorityName(),user.getAuthorityType());
		userName=user.getAuthorityDisplayName();
		profile=new UserProfile(user);
	}

	public static UserSimple getDummy(String name) {
		UserSimple userSimple = new UserSimple();
		userSimple.setAuthorityName(name);
		userSimple.setAuthorityType(Type.USER);
		return userSimple;
	}

	/**
	 **/
	@Schema(description = "")
	@JsonProperty("userName")
	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	/**
	 **/
	@Schema(description = "")
	@JsonProperty("profile")
	public UserProfile getProfile() {
		return profile;
	}

	public void setProfile(UserProfile profile) {
		this.profile = profile;
	}

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public UserStatus getStatus() {
        return status;
    }

	@JsonProperty
	public List<Organization> getOrganizations() {
		return organizations;
	}

	public void setOrganizations(List<Organization> organizations) {
		this.organizations = organizations;
	}

}
