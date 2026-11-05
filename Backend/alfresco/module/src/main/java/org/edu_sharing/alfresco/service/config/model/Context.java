package org.edu_sharing.alfresco.service.config.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlElement;

import java.io.Serializable;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Context implements Serializable {
	@XmlElement	public String id;
	@XmlElement	public String[] domain;
	@XmlElement	public Values values;
	@XmlElement	public List<Language> language;
	@XmlElement	public Variables variables;
	@Schema(description = "Scope group (authority name, groupType SCOPE) that is the active scope (editorial team) of this context")
	@XmlElement	public String scopeGroup;

}
