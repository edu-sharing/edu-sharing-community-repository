package org.edu_sharing.restservices.shared;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.AnnotationIntrospector.ReferenceProperty.Type;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.Map;

@ApiModel(description = "")
public class Authority {

	public static enum Type {USER, GROUP, OWNER, EVERYONE, GUEST;};

	private String authorityName;
	private Type authorityType;
	private Map<String, String[]> properties;

	public Authority(){}
	public Authority(String authorityName, String authorityType) {
		this.authorityName=authorityName;
		switch(authorityType){
			case "USER":
				this.authorityType=Type.USER;
				break;
			case "GROUP":
				this.authorityType=Type.GROUP;
				break;
			case "OWNER":
				this.authorityType=Type.OWNER;
				break;
			case "GUEST":
				this.authorityType=Type.GUEST;
				break;
			default:
				this.authorityType=Type.EVERYONE;
		}
	}
	public Authority(String authorityName, Type authorityType) {
		this.authorityName=authorityName;
		this.authorityType=authorityType;
	}
	@ApiModelProperty(required = true, value = "")
	@JsonProperty("authorityName")
	public String getAuthorityName() {
		return authorityName;
	}

	public void setAuthorityName(String groupName) {
		this.authorityName = groupName;
	}

	/**
	 **/
	@ApiModelProperty(value = "")
	@JsonProperty("authorityType")
	public Type getAuthorityType() {
		return authorityType;
	}

	public void setAuthorityType(Type authorityType) {
		this.authorityType = authorityType;
	}


	@JsonProperty
	public Map<String, String[]> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, String[]> properties) {
		this.properties = properties;
	}
	
	@Override
	public boolean equals(Object obj) {
		
		if(!(obj instanceof Authority)){
			return false;
		}
		
		if(obj == null) return false;
		
		Authority toCompare = (Authority)obj;
		if(this.getAuthorityName().equals(toCompare.getAuthorityName()) &&
				this.getAuthorityType().equals(toCompare.getAuthorityType())){
			return true;
		}
		
		return false;
	}
	
	@Override
	public int hashCode() {
		int hash = 7;
		hash = 31 * hash + (null == authorityName ? 0 : authorityName.hashCode());
		hash = 31 * hash + (null == authorityType ? 0 : authorityType.hashCode());
		return hash;
	}
}
