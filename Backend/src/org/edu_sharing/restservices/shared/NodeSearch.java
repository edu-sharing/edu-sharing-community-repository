package org.edu_sharing.restservices.shared;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

@Schema(description = "")
public class NodeSearch {

	@Schema(description = "")
	public static class Facette {

		@Schema(description = "")
		public static class Value {

			private String value = null;
			private Integer count = null;

			@Schema(required = true, description = "")
			@JsonProperty(value = "value")
			public String getValue() {
				return value;
			}

			public void setValue(String value) {
				this.value = value;
			}

			@Schema(required = true, description = "")
			@JsonProperty(value = "count")
			public Integer getCount() {
				return count;
			}

			public void setCount(Integer count) {
				this.count = count;
			}
		}

		private String property = null;
		private List<Value> values = null;
		private Long sumOtherDocCount = null;

		@Schema(required = true, description = "")
		@JsonProperty(value = "property")
		public String getProperty() {
			return property;
		}

		public void setProperty(String property) {
			this.property = property;
		}

		@Schema(required = true, description = "")
		@JsonProperty(value = "values")
		public List<Value> getValues() {
			return values;
		}

		public void setValues(List<Value> values) {
			this.values = values;
		}

		public void setSumOtherDocCount(Long sumOtherDocCount) { this.sumOtherDocCount = sumOtherDocCount; }

		@Schema(required = false, defaultValue = "")
		@JsonProperty(value = "sumOtherDocCount")
		public Long getSumOtherDocCount() { return sumOtherDocCount; }
	}
	
	private List<String> ignored = null;
	private List<NodeRef> result = null;
	private List<Facette> facettes = null;
	private Integer count = null;
	private Integer skip = null;
	private List<Node> nodes = null;

	/**
   **/
	@Schema(required = true, description = "")
	@JsonProperty(value = "result")
	public List<NodeRef> getResult() {
		return result;
	}
	
	public void setResult(List<NodeRef> result) {
		this.result = result;
	}
	
	@JsonProperty(value = "ignored")
	public List<String> getIgnored() {
		return ignored;
	}

	public void setIgnored(List<String> ignored) {
		this.ignored = ignored;
	}

	/**
	   **/
	@Schema(required = true, description = "")
	@JsonProperty(value = "facettes")
	public List<Facette> getFacettes() {
		return facettes;
	}

	public void setFacettes(List<Facette> facettes) {
		this.facettes = facettes;
	}

	/**
   **/
	@Schema(required = true, description = "")
	@JsonProperty(value = "count")
	public Integer getCount() {
		return count;
	}

	public void setCount(Integer count) {
		this.count = count;
	}

	/**
   **/
	@Schema(required = true, description = "")
	@JsonProperty(value = "skip")
	public Integer getSkip() {
		return skip;
	}

	public void setSkip(Integer skip) {
		this.skip = skip;
	}
	
	public void setNodes(List<Node> data) {
		this.nodes = data;
	}
	
	public List<Node> getNodes() {
		return nodes;
	}

}
