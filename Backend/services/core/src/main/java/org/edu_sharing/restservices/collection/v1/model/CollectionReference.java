package org.edu_sharing.restservices.collection.v1.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import org.apache.commons.beanutils.PropertyUtils;
import org.edu_sharing.restservices.shared.Node;
import org.edu_sharing.restservices.shared.Preview;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

@ApiModel(description = "")
public class CollectionReference extends Node {

	private String originalId;

	private List<String> accessOriginal;
	private boolean originalRestrictedAccess;

	public CollectionReference(){

	}
	public CollectionReference(Node node) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
		PropertyUtils.copyProperties(this, node);
	}

	@ApiModelProperty(required = false, value = "")
	@JsonProperty("originalId")
	public String getOriginalId() {
		return originalId;
	}

	public void setOriginalId(String originalId) {
		this.originalId = originalId;
	}

	@JsonProperty
	public void setAccessOriginal(List<String> accessOriginal) {
		this.accessOriginal = accessOriginal;
	}

	public List<String> getAccessOriginal() {
		return accessOriginal;
	}

    public void setOriginalRestrictedAccess(boolean originalRestrictedAccess) {
        this.originalRestrictedAccess = originalRestrictedAccess;
    }

    public boolean getOriginalRestrictedAccess() {
        return originalRestrictedAccess;
    }
}
