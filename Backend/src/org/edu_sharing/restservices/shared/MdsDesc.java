package org.edu_sharing.restservices.shared;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;;

import com.fasterxml.jackson.annotation.JsonProperty;

@Schema(description = "")
public class MdsDesc {

	private MdsRef ref = null;
	private Boolean isDefaultMds = null;

	@Schema(required = true, description = "")
	@JsonProperty("ref")
	public MdsRef getRef() {
		return ref;
	}

	public void setRef(MdsRef ref) {
		this.ref = ref;
	}

	/**
	   **/
	  @Schema(required = true, description = "")
	  @JsonProperty("isDefaultMds")
	  public Boolean isDefaultMds() {
	    return isDefaultMds;
	  }
	  public void setDefaultMds(Boolean isDefaultMds) {
	    this.isDefaultMds = isDefaultMds;
	  }


}
