package org.edu_sharing.restservices.collection.v1.model;

import org.edu_sharing.restservices.shared.Node;
import org.edu_sharing.restservices.shared.Preview;
import org.edu_sharing.restservices.shared.User;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;;import java.io.Serializable;

@Schema(description = "")
public class Collection implements Serializable {

	boolean level0;

	String title;
	String description;
	String type;
	String viewtype;
	String orderMode;
	
	int x;
	int y;
	int z;
	
	String color;

	User owner;	
	
	boolean fromUser;
	boolean pinned;
	int childCollectionsCount;
	
	int childReferencesCount;


	private String scope;
	private String authorFreetext;



	@Schema(required = true, description = "false")
	@JsonProperty("level0")
	public boolean isLevel0() {
		return level0;
	}

	public void setLevel0(boolean level0) {
		this.level0 = level0;
	}
	@Schema(required = true, description = "false")
	@JsonProperty("fromUser")
	public boolean isFromUser() {
		return fromUser;
	}

	public void setFromUser(boolean fromUser) {
		this.fromUser = fromUser;
	}
	@Schema(required = true, description = "")
	@JsonProperty("title")
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	@Schema(required = false)
	@JsonProperty("description")
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Schema(required = true, description = "")
	@JsonProperty("type")
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@Schema(required = true, description = "")
	@JsonProperty("viewtype")
	public String getViewtype() {
		return viewtype;
	}

	public void setViewtype(String viewtype) {
		this.viewtype = viewtype;
	}

	@Schema(required = false)
	@JsonProperty("x")
	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}

	@Schema(required = false)
	@JsonProperty("y")
	public int getY() {
		return y;
	}

	public void setY(int y) {
		this.y = y;
	}

	@Schema(required = false)
	@JsonProperty("z")
	public int getZ() {
		return z;
	}

	public void setZ(int z) {
		this.z = z;
	}

	@Schema(required = false)
	@JsonProperty("color")
	public String getColor() {
		return color;
	}

	public void setColor(String color) {
		this.color = color;
	}

	@Schema(required = false)
	@JsonProperty("childCollectionsCount")
	public int getChildCollectionsCount() {
		return childCollectionsCount;
	}
	
	public void setChildCollectionsCount(int childCollectionsCount) {
		this.childCollectionsCount = childCollectionsCount;
	}
	
	@Schema(required = false)
	@JsonProperty("childReferencesCount")
	public int getChildReferencesCount() {
		return childReferencesCount;
	}
	
	public void setChildReferencesCount(int childReferencesCount) {
		this.childReferencesCount = childReferencesCount;
	}
	
	@Schema(required = false)
	@JsonProperty("pinned")
	public boolean isPinned() {
		return pinned;
	}

	public void setPinned(boolean pinned) {
		this.pinned = pinned;
	}
	
	@Schema(required = false)
	@JsonProperty("orderMode")
	public String getOrderMode() {
		return orderMode;
	}

	public void setOrderMode(String orderMode) {
		this.orderMode = orderMode;
	}

	@JsonProperty
	public String getScope() {
		return scope;
	}

	public void setScope(String scope) {
		this.scope = scope;
	}

	@JsonProperty
	public String getAuthorFreetext() {
		return authorFreetext;
	}

	public void setAuthorFreetext(String authorFreetext) {
		this.authorFreetext = authorFreetext;
	}
}
