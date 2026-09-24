package org.edu_sharing.restservices.config.v1.model;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.edu_sharing.alfresco.service.config.model.Language;
import org.edu_sharing.alfresco.service.config.model.Values;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.edu_sharing.alfresco.service.config.model.ValuesBackend;
import org.edu_sharing.alfresco.service.config.model.ValuesBackendInternal;

@Data
public class Config {
	private String contextId;
	@JsonPropertyDescription("Relevant config infos mapped from the Lightbend from the backend")
	@Schema(description = "Relevant config infos mapped from the Lightbend from the backend. "
			+ "ValuesBackendInternal (a superset) for internal requests; ValuesBackend otherwise",
			oneOf = {ValuesBackend.class, ValuesBackendInternal.class})
	public ValuesBackend currentBackend;
	private Values current;
	private Values global;
	private Language language;
}
