package org.edu_sharing.restservices.shared;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.edu_sharing.restservices.UserStatus;

@ApiModel(description = "")
public class User extends UserSimple {

	
	private NodeRef homeFolder = null;
	private List<NodeRef> sharedFolders = new ArrayList<NodeRef>();
	private UserQuota quota;

	public User(){super();}
	public User(org.edu_sharing.repository.client.rpc.User user) {
		super(user);
		homeFolder=new NodeRef(user.getRepositoryId(),user.getNodeId());
	}

	@ApiModelProperty(required = true, value = "")
	@JsonProperty("homeFolder")
	public NodeRef getHomeFolder() {
		return homeFolder;
	}

	public void setHomeFolder(NodeRef homeFolder) {
		this.homeFolder = homeFolder;
	}

	@ApiModelProperty(value = "")
	@JsonProperty("sharedFolders")
	public List<NodeRef> getSharedFolders() {
		return sharedFolders;
	}

	public void setSharedFolders(List<NodeRef> sharedFolders) {
		this.sharedFolders = sharedFolders;
	}

	@JsonProperty
	public UserQuota getQuota() {
		return quota;
	}

	public void setQuota(UserQuota quota) {
		this.quota = quota;
	}


}
