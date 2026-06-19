package org.edu_sharing.alfresco.service.config.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlElement;
import java.io.Serializable;

public class Collections implements Serializable {
	public enum CollectionsInvitationType {
		@Schema(description = "Default invitation dialog for inviting any group or user")
		Default,
		@Schema(description = "List of editorial groups only")
		EditorialGroups
	}
	public static class CollectionsTypeConfig implements Serializable {
		@Schema(description = "Type of invitation dialog")
		@XmlElement public CollectionsInvitationType invitationType;
		@Schema(description = "Metadata group ID to display (null = no metadata shown)")
		@XmlElement public String metadataGroup;
	}
	public static class CollectionsType implements Serializable {
		@Schema(description = "Configuration for editorial collections")
		@XmlElement public CollectionsTypeConfig editorial;
	}
	@Schema(description = "Special collection types configuration")
	@XmlElement public CollectionsType types;
	@Schema(description = "Array of allowed color values for collections")
	@XmlElement public String[] colors;
}
