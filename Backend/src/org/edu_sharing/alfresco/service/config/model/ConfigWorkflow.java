package org.edu_sharing.alfresco.service.config.model;

import javax.xml.bind.annotation.XmlElement;
import java.io.Serializable;

public class ConfigWorkflow implements Serializable {

	@XmlElement
	public String defaultReceiver;
	@XmlElement
	public String defaultStatus;
	@XmlElement
	public Boolean commentRequired;
	@XmlElement
	public ConfigWorkflowList[] workflows;

	public static class ConfigWorkflowList {
		@XmlElement
		public String id;
		@XmlElement
		public String color;
		@XmlElement
		public Boolean hasReceiver;
		@XmlElement
		public String[] next;
	}
}
