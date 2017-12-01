package org.edu_sharing.restservices.mds.v1.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;

public class Suggestions {

	private List<Suggestion> values;
	
	@ApiModelProperty(required = true, value = "")
	@JsonProperty("values")
	public List<Suggestion> getValues() {
		return values;
	}
	
	public void setValues(List<Suggestion> values) {
		this.values = values;
	}
	
	public static class Suggestion{
		String replacementString;
		String displayString;
		
		String key;
		
		
		@ApiModelProperty(required = true, value = "")
		@JsonProperty("displayString")
		public String getDisplayString() {
			return displayString;
		}
		
		@ApiModelProperty(required = true, value = "")
		@JsonProperty("replacementString")
		public String getReplacementString() {
			return replacementString;
		}
		
		@ApiModelProperty(required = false, value = "")
		@JsonProperty("key")
		public String getKey() {
			return key;
		}
		
		public void setKey(String key) {
			this.key = key;
		}
		
		public void setDisplayString(String displayString) {
			this.displayString = displayString;
		}
		
		public void setReplacementString(String replacementString) {
			this.replacementString = replacementString;
		}
	}
}
