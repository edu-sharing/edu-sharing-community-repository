package org.edu_sharing.restservices.config.v1.model;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Data;
import org.edu_sharing.alfresco.service.config.model.Language;
import org.edu_sharing.alfresco.service.config.model.Values;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.edu_sharing.alfresco.service.config.model.ValuesBackend;

@Data
public class Config {
	private String contextId;
	@JsonPropertyDescription("Relevant config infos mapped from the Lightbend from the backend")
	public ValuesBackend currentBackend;
	private Values current;
	private Values global;
	private Language language;
}
