package org.edu_sharing.restservices.collection.v1.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import org.edu_sharing.restservices.shared.Node;
import org.edu_sharing.restservices.shared.Preview;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@ApiModel(description = "")
public class CollectionReference extends CollectionBase {

	private String originalId;
	
	private Node reference;
	

	private Preview preview;
	private List<String> accessOriginal;

	@ApiModelProperty(required = false, value = "")
	@JsonProperty("originalId")
	public String getOriginalId() {
		return originalId;
	}
	
	public void setOriginalId(String originalId) {
		this.originalId = originalId;
	}
	
	public void setReference(Node reference) {
		this.reference = reference;
	}
	
	@ApiModelProperty(required = true, value = "")
	@JsonProperty("reference")
	public Node getReference() {
		return reference;
	}
	
	
	public void setPreview(Preview preview) {
		this.preview = preview;
	}
	
	@ApiModelProperty(required = true, value = "")
	@JsonProperty("preview")
	public Preview getPreview() {
		return preview;
	}

	@JsonProperty
    public void setAccessOriginal(List<String> accessOriginal) {
        this.accessOriginal = accessOriginal;
    }

    public List<String> getAccessOriginal() {
        return accessOriginal;
    }
}
