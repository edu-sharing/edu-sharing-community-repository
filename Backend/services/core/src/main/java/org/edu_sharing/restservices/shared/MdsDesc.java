package org.edu_sharing.restservices.shared;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import com.fasterxml.jackson.annotation.JsonProperty;

@ApiModel(description = "")
public class MdsDesc {

	private MdsRef ref = null;
	private Boolean isDefaultMds = null;

	@ApiModelProperty(required = true, value = "")
	@JsonProperty("ref")
	public MdsRef getRef() {
		return ref;
	}

	public void setRef(MdsRef ref) {
		this.ref = ref;
	}

	/**
	   **/
	  @ApiModelProperty(required = true, value = "")
	  @JsonProperty("isDefaultMds")
	  public Boolean isDefaultMds() {
	    return isDefaultMds;
	  }
	  public void setDefaultMds(Boolean isDefaultMds) {
	    this.isDefaultMds = isDefaultMds;
	  }


}
