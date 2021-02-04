package org.edu_sharing.restservices.shared;

import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.admin.model.GlobalGroup;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.edu_sharing.service.organization.GroupSignupMethod;

import java.util.List;

@ApiModel(description = "")
public class Group extends Authority {

	private GroupSignupMethod signupMethod;
	private String groupName = null;
	private GroupProfile profile = null;
	private NodeRef ref = null;

	/**
	 * editable in context of grouptype and loction(shared nodes)
	 * 
	 * @TODO is an prop for the ACE Object, remove when not longer needed
	 */
	private boolean editable = true;
	private List<String> aspects;

	private List<Organization> organizations;

	public Group(){
	}
	public static Group getEveryone(){
		Group group=new Group();
		group.setAuthorityName(CCConstants.AUTHORITY_GROUP_EVERYONE);
		group.setAuthorityType(Type.EVERYONE);
		return group;
	}
	public Group(GlobalGroup group) {
		groupName=group.getName();
		setAuthorityType(Type.GROUP);
		setGroupName(group.getName());

    	GroupProfile profile = new GroupProfile();
    	profile.setDisplayName(group.getDisplayName());
    	profile.setGroupType(group.getGroupType());
    	setProfile(profile);
    	
	}

	@ApiModelProperty(value = "")
	@JsonProperty("groupName")
	public String getGroupName() {
		return groupName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}
	/**
	 **/
	@ApiModelProperty(value = "")
	@JsonProperty("profile")
	public GroupProfile getProfile() {
		return profile;
	}

	public void setProfile(GroupProfile profile) {
		this.profile = profile;
	}
	
	public boolean isEditable() {
		return editable;
	}
	
	public void setEditable(boolean editable) {
		this.editable = editable;
	}
	@JsonProperty
	public NodeRef getRef() {
		return ref;
	}
	public void setRef(NodeRef ref) {
		this.ref = ref;
	}


    public void setAspects(List<String> aspects) {
        this.aspects = aspects;
    }

    public List<String> getAspects() {
        return aspects;
    }
	@JsonProperty
	public List<Organization> getOrganizations() {
		return organizations;
	}

	public void setOrganizations(List<Organization> organizations) {
		this.organizations = organizations;
	}

	@JsonProperty
	public GroupSignupMethod getSignupMethod() {
		return signupMethod;
	}

	public void setSignupMethod(GroupSignupMethod signupMethod) {
		this.signupMethod = signupMethod;
	}

}
