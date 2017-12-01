package org.edu_sharing.restservices.collection.v1.model;

import org.edu_sharing.restservices.shared.Preview;
import org.edu_sharing.restservices.shared.User;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "")
public class Collection extends CollectionBase {

	boolean level0;

	String title;
	String description;
	String type;
	String viewtype;
	
	int x;
	int y;
	int z;
	
	String color;

	User owner;	
	
	boolean fromUser;
	int childCollectionsCount;
	
	int childReferencesCount;
	
	
	@ApiModelProperty(required = true, value = "false")
	@JsonProperty("level0")
	public boolean isLevel0() {
		return level0;
	}

	public void setLevel0(boolean level0) {
		this.level0 = level0;
	}
	@ApiModelProperty(required = true, value = "false")
	@JsonProperty("fromUser")
	public boolean isFromUser() {
		return fromUser;
	}

	public void setFromUser(boolean fromUser) {
		this.fromUser = fromUser;
	}
	@ApiModelProperty(required = true, value = "")
	@JsonProperty("title")
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	@ApiModelProperty(required = false)
	@JsonProperty("description")
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@ApiModelProperty(required = true, value = "")
	@JsonProperty("type")
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@ApiModelProperty(required = true, value = "")
	@JsonProperty("viewtype")
	public String getViewtype() {
		return viewtype;
	}

	public void setViewtype(String viewtype) {
		this.viewtype = viewtype;
	}

	@ApiModelProperty(required = false)
	@JsonProperty("x")
	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}

	@ApiModelProperty(required = false)
	@JsonProperty("y")
	public int getY() {
		return y;
	}

	public void setY(int y) {
		this.y = y;
	}

	@ApiModelProperty(required = false)
	@JsonProperty("z")
	public int getZ() {
		return z;
	}

	public void setZ(int z) {
		this.z = z;
	}

	@ApiModelProperty(required = false)
	@JsonProperty("color")
	public String getColor() {
		return color;
	}

	public void setColor(String color) {
		this.color = color;
	}

	@ApiModelProperty(required = false)
	@JsonProperty("owner")
	public User getOwner() {
		return owner;
	}
	
	public void setOwner(User owner) {
		this.owner = owner;
	}
	
	@ApiModelProperty(required = false)
	@JsonProperty("childCollectionsCount")
	public int getChildCollectionsCount() {
		return childCollectionsCount;
	}
	
	public void setChildCollectionsCount(int childCollectionsCount) {
		this.childCollectionsCount = childCollectionsCount;
	}
	
	@ApiModelProperty(required = false)
	@JsonProperty("childReferencesCount")
	public int getChildReferencesCount() {
		return childReferencesCount;
	}
	
	public void setChildReferencesCount(int childReferencesCount) {
		this.childReferencesCount = childReferencesCount;
	}

}
