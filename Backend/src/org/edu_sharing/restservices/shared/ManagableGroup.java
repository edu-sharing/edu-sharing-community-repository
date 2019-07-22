package org.edu_sharing.restservices.shared;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "")
public class ManagableGroup extends Group {

	private boolean administrationAccess;

	@ApiModelProperty(value = "")
	@JsonProperty("administrationAccess")
	public boolean getAdministrationAccess() {
		return administrationAccess;
	}

	public void setAdministrationAccess(boolean administrationAccess) {
		this.administrationAccess = administrationAccess;
	}
}
