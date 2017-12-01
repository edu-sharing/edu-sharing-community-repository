package org.edu_sharing.restservices.shared;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

@ApiModel(description = "")
public class MdsType {

	private String type = null;
	private List<MdsProperty> properties = null;

	@ApiModelProperty(required = true, value = "")
	@JsonProperty("type")
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@ApiModelProperty(required = true, value = "")
	@JsonProperty("properties")
	public List<MdsProperty> getProperties() {
		return properties;
	}

	public void setProperties(List<MdsProperty> properties) {
		this.properties = properties;
	}

	@ApiModel(description = "")
	public static class MdsProperty {

		private String name = null;
		private String type = null;
		private String defaultValue = null;
		private String processtype = null;
		private String keyContenturl = null;
		private Boolean concatewithtype = null;
		private Boolean multiple = null;
		private String copyFrom = null;

		
		@ApiModelProperty(required = true, value = "")
		@JsonProperty("name")
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}

		@ApiModelProperty(required = true, value = "")
		@JsonProperty("type")
		public String getType() {
			return type;
		}
		public void setType(String type) {
			this.type = type;
		}

		@ApiModelProperty(required = true, value = "")
		@JsonProperty("defaultValue")
		public String getDefaultValue() {
			return defaultValue;
		}
		public void setDefaultValue(String defaultValue) {
			this.defaultValue = defaultValue;
		}

		@ApiModelProperty(required = true, value = "")
		@JsonProperty("processtype")
		public String getProcesstype() {
			return processtype;
		}
		public void setProcesstype(String processtype) {
			this.processtype = processtype;
		}

		@ApiModelProperty(required = true, value = "")
		@JsonProperty("keyContenturl")
		public String getKeyContenturl() {
			return keyContenturl;
		}
		public void setKeyContenturl(String keyContenturl) {
			this.keyContenturl = keyContenturl;
		}

		@ApiModelProperty(required = true, value = "")
		@JsonProperty("concatewithtype")
		public Boolean getConcatewithtype() {
			return concatewithtype;
		}
		public void setConcatewithtype(Boolean concatewithtype) {
			this.concatewithtype = concatewithtype;
		}

		@ApiModelProperty(required = true, value = "")
		@JsonProperty("multiple")
		public Boolean getMultiple() {
			return multiple;
		}
		public void setMultiple(Boolean multiple) {
			this.multiple = multiple;
		}

		@ApiModelProperty(required = true, value = "")
		@JsonProperty("copyFrom")
		public String getCopyFrom() {
			return copyFrom;
		}
		public void setCopyFrom(String copyFrom) {
			this.copyFrom = copyFrom;
		}


	}

}
