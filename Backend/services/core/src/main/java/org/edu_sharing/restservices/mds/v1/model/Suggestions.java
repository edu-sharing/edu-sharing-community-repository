package org.edu_sharing.restservices.mds.v1.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;;

public class Suggestions {

	private List<Suggestion> values;
	
	@Schema(required = true, description = "")
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
		
		
		@Schema(required = true, description = "")
		@JsonProperty("displayString")
		public String getDisplayString() {
			return displayString;
		}
		
		@Schema(required = true, description = "")
		@JsonProperty("replacementString")
		public String getReplacementString() {
			return replacementString;
		}
		
		@Schema(required = false, description = "")
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
