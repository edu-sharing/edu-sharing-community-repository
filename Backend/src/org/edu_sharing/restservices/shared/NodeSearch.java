package org.edu_sharing.restservices.shared;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

@ApiModel(description = "")
public class NodeSearch {

	@ApiModel(description = "")
	public static class Facette {

		@ApiModel(description = "")
		public static class Value {

			private String value = null;
			private Integer count = null;

			@ApiModelProperty(required = true, value = "")
			@JsonProperty(value = "value")
			public String getValue() {
				return value;
			}

			public void setValue(String value) {
				this.value = value;
			}

			@ApiModelProperty(required = true, value = "")
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

		@ApiModelProperty(required = true, value = "")
		@JsonProperty(value = "property")
		public String getProperty() {
			return property;
		}

		public void setProperty(String property) {
			this.property = property;
		}

		@ApiModelProperty(required = true, value = "")
		@JsonProperty(value = "values")
		public List<Value> getValues() {
			return values;
		}

		public void setValues(List<Value> values) {
			this.values = values;
		}
	}
	
	private List<String> ignored = null;
	private List<NodeRef> result = null;
	private List<Facette> facettes = null;
	private Integer count = null;
	private Integer skip = null;
	private List<Node> nodes = null;

	/**
   **/
	@ApiModelProperty(required = true, value = "")
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
	@ApiModelProperty(required = true, value = "")
	@JsonProperty(value = "facettes")
	public List<Facette> getFacettes() {
		return facettes;
	}

	public void setFacettes(List<Facette> facettes) {
		this.facettes = facettes;
	}

	/**
   **/
	@ApiModelProperty(required = true, value = "")
	@JsonProperty(value = "count")
	public Integer getCount() {
		return count;
	}

	public void setCount(Integer count) {
		this.count = count;
	}

	/**
   **/
	@ApiModelProperty(required = true, value = "")
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
