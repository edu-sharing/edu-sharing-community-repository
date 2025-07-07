package org.edu_sharing.alfresco.service.config.model;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.xml.bind.annotation.XmlElement;
import java.io.Serializable;

public class Collections implements Serializable {
	public enum CollectionsInvitationType {
		@JsonPropertyDescription("Default invitation dialog, you can invite every group or user")
		Default,
		@JsonPropertyDescription("List of editorial groups only")
		EditorialGroups
	}
	public static class CollectionsTypeConfig implements Serializable {
		@JsonPropertyDescription("Choose the invitation type")
		@XmlElement	public CollectionsInvitationType invitationType;
		@JsonPropertyDescription("set the metadata group id (or null if no metadata should be shown)")
		@XmlElement	public String metadataGroup;
	}
	public static class CollectionsType implements Serializable {
		@XmlElement	public CollectionsTypeConfig editorial;
	}
	@XmlElement	public CollectionsType types;
	@XmlElement	public String[] colors;
}
