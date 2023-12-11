package org.edu_sharing.restservices.shared;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

@Schema(description = "")
public class NodeSearch {

	@Schema(description = "")
	public static class Facet {

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
		private List<Value> values = new ArrayList<>();
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

	public static class Suggest{
		String text;
		String highlighted;
		double score;

		public void setHighlighted(String highlighted) {
			this.highlighted = highlighted;
		}

		@Schema(required = true, description = "suggested text")
		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}

		@Schema(required = false, description = "suggested text with corrected words highlighted")
		public String getHighlighted() {
			return highlighted;
		}

		public void setScore(double score) {
			this.score = score;
		}

		@Schema(required = true, description = "score of the suggestion")
		public double getScore() {
			return score;
		}
	}
	
	private List<String> ignored = null;
	private List<NodeRef> result = null;
	private List<Facet> facets = null;
	private List<Suggest> suggests = null;
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
	@JsonProperty(value = "facets")
	public List<Facet> getFacets() {
		return facets;
	}

	public void setFacets(List<Facet> facets) {
		this.facets = facets;
	}

	public void setSuggests(List<Suggest> suggests) { this.suggests = suggests; }

	public List<Suggest> getSuggests() { return suggests; }

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
