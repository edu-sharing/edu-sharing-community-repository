package org.edu_sharing.restservices.shared;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;;

@Schema(description = "")
public class ManagableGroup extends Group {

	private boolean administrationAccess;

	@Schema(description = "")
	@JsonProperty("administrationAccess")
	public boolean getAdministrationAccess() {
		return administrationAccess;
	}

	public void setAdministrationAccess(boolean administrationAccess) {
		this.administrationAccess = administrationAccess;
	}
}
