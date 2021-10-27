package org.edu_sharing.restservices.shared;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

@Schema(description = "")
public class MdsType {

	private String type = null;
	private List<MdsProperty> properties = null;

	@Schema(required = true, description = "")
	@JsonProperty("type")
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@Schema(required = true, description = "")
	@JsonProperty("properties")
	public List<MdsProperty> getProperties() {
		return properties;
	}

	public void setProperties(List<MdsProperty> properties) {
		this.properties = properties;
	}

	@Schema(description = "")
	public static class MdsProperty {

		private String name = null;
		private String type = null;
		private String defaultValue = null;
		private String processtype = null;
		private String keyContenturl = null;
		private Boolean concatewithtype = null;
		private Boolean multiple = null;
		private String copyFrom = null;

		
		@Schema(required = true, description = "")
		@JsonProperty("name")
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}

		@Schema(required = true, description = "")
		@JsonProperty("type")
		public String getType() {
			return type;
		}
		public void setType(String type) {
			this.type = type;
		}

		@Schema(required = true, description = "")
		@JsonProperty("defaultValue")
		public String getDefaultValue() {
			return defaultValue;
		}
		public void setDefaultValue(String defaultValue) {
			this.defaultValue = defaultValue;
		}

		@Schema(required = true, description = "")
		@JsonProperty("processtype")
		public String getProcesstype() {
			return processtype;
		}
		public void setProcesstype(String processtype) {
			this.processtype = processtype;
		}

		@Schema(required = true, description = "")
		@JsonProperty("keyContenturl")
		public String getKeyContenturl() {
			return keyContenturl;
		}
		public void setKeyContenturl(String keyContenturl) {
			this.keyContenturl = keyContenturl;
		}

		@Schema(required = true, description = "")
		@JsonProperty("concatewithtype")
		public Boolean getConcatewithtype() {
			return concatewithtype;
		}
		public void setConcatewithtype(Boolean concatewithtype) {
			this.concatewithtype = concatewithtype;
		}

		@Schema(required = true, description = "")
		@JsonProperty("multiple")
		public Boolean getMultiple() {
			return multiple;
		}
		public void setMultiple(Boolean multiple) {
			this.multiple = multiple;
		}

		@Schema(required = true, description = "")
		@JsonProperty("copyFrom")
		public String getCopyFrom() {
			return copyFrom;
		}
		public void setCopyFrom(String copyFrom) {
			this.copyFrom = copyFrom;
		}


	}

}
