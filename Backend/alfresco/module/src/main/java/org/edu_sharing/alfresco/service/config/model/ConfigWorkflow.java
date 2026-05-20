package org.edu_sharing.alfresco.service.config.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlElement;
import java.io.Serializable;

public class ConfigWorkflow implements Serializable {

	@Schema(description = "Default group/user pre-filled as responsible party")
	@XmlElement
	public String defaultReceiver;
	@Schema(description = "Default target status pre-filled in workflow dialog")
	@XmlElement
	public String defaultStatus;
	@Schema(description = "If true (default), comment is required in workflow dialog")
	@XmlElement
	public Boolean commentRequired;
	@Schema(description = "Workflow status definitions")
	@XmlElement
	public ConfigWorkflowList[] workflows;

	public static class ConfigWorkflowList implements Serializable {
		@Schema(description = "Status identifier (typically numeric, e.g. '100_unchecked')")
		@XmlElement
		public String id;
		@Schema(description = "HTML color code for this status")
		@XmlElement
		public String color;
		@Schema(description = "If true, receiver can be set for this status (false for release states)")
		@XmlElement
		public Boolean hasReceiver;
		@Schema(description = "Array of status IDs allowed as next states (client-side validation only)")
		@XmlElement
		public String[] next;
	}
}
