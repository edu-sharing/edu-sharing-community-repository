package org.edu_sharing.restservices.shared;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

@Schema(description = "")
public class MdsList {

	private String id = null;
	private String label = null;
	private List<MdsListProperty> properties = null;
	
	@Schema(required = true, description = "")
	@JsonProperty("id")
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}

	@Schema(required = true, description = "")
	@JsonProperty("label")
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}

	@Schema(required = true, description = "")
	@JsonProperty("properties")	
	public List<MdsListProperty> getProperties() {
		return properties;
	}
	public void setProperties(List<MdsListProperty> properties) {
		this.properties = properties;
	}
	

	@Schema(description = "")
	public static class MdsListProperty {

		private String name = null;
		private String label = null;
		private String labelHint = null;
		private String formHeight = null;
		private String formLength = null;
		private String widget = null;
		private String widgetTitle = null;
		private List<String> copyFrom = null;
		private List<MdsListPropertyParameter> parameters = null;
		private List<MdsListPropertyValue> values = null;
		private List<String> defaultValues = null;
		private Boolean multiple = null;
		private String placeHolder = null;
		private String styleName = null;
		private String styleNameLabel = null;
		private String type = null;
		
		@Schema(required = true, description = "")
		@JsonProperty("name")
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Schema(required = true, description = "")
		@JsonProperty("label")
		public String getLabel() {
			return label;
		}

		public void setLabel(String label) {
			this.label = label;
		}

		@Schema(required = true, description = "")
		@JsonProperty("labelHint")
		public String getLabelHint() {
			return labelHint;
		}

		public void setLabelHint(String labelHint) {
			this.labelHint = labelHint;
		}

		@Schema(required = true, description = "")
		@JsonProperty("formHeight")
		public String getFormHeight() {
			return formHeight;
		}

		public void setFormHeight(String formHeight) {
			this.formHeight = formHeight;
		}

		@Schema(required = true, description = "")
		@JsonProperty("formLength")
		public String getFormLength() {
			return formLength;
		}

		public void setFormLength(String formLength) {
			this.formLength = formLength;
		}

		@Schema(required = true, description = "")
		@JsonProperty("widget")
		public String getWidget() {
			return widget;
		}

		public void setWidget(String widget) {
			this.widget = widget;
		}

		@Schema(required = true, description = "")
		@JsonProperty("widgetTitle")
		public String getWidgetTitle() {
			return widgetTitle;
		}

		public void setWidgetTitle(String widgetTitle) {
			this.widgetTitle = widgetTitle;
		}

		@Schema(required = true, description = "")
		@JsonProperty("copyFrom")
		public List<String> getCopyFrom() {
			return copyFrom;
		}

		public void setCopyFrom(List<String> copyFrom) {
			this.copyFrom = copyFrom;
		}

		@Schema(required = true, description = "")
		@JsonProperty("parameters")
		public List<MdsListPropertyParameter> getParameters() {
			return parameters;
		}

		public void setParameters(List<MdsListPropertyParameter> parameters) {
			this.parameters = parameters;
		}

		@Schema(required = true, description = "")
		@JsonProperty("values")
		public List<MdsListPropertyValue> getValues() {
			return values;
		}
		public void setValues(List<MdsListPropertyValue> values) {
			this.values = values;
		}

		@Schema(required = true, description = "")
		@JsonProperty("defaultValues")
		public List<String> getDefaultValues() {
			return defaultValues;
		}

		public void setDefaultValues(List<String> defaultValues) {
			this.defaultValues = defaultValues;
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
		@JsonProperty("placeHolder")
		public String getPlaceHolder() {
			return placeHolder;
		}

		public void setPlaceHolder(String placeHolder) {
			this.placeHolder = placeHolder;
		}

		@Schema(required = true, description = "")
		@JsonProperty("styleName")		
		public String getStyleName() {
			return styleName;
		}

		public void setStyleName(String styleName) {
			this.styleName = styleName;
		}

		@Schema(required = true, description = "")
		@JsonProperty("styleNameLabel")
		public String getStyleNameLabel() {
			return styleNameLabel;
		}

		public void setStyleNameLabel(String styleNameLabel) {
			this.styleNameLabel = styleNameLabel;
		}

		@Schema(required = true, description = "")
		@JsonProperty("type")
		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

	}
	
	@Schema(description = "")
	public static class MdsListPropertyParameter {

		private String name = null;
		private String value = null;
		
		@Schema(required = true, description = "")
		@JsonProperty("name")
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}

		@Schema(required = true, description = "")
		@JsonProperty("value")
		public String getValue() {
			return value;
		}
		public void setValue(String value) {
			this.value = value;
		}
	}

	@Schema(description = "")
	public static class MdsListPropertyValue {

		private String key = null;
		private String value = null;
		
		@Schema(required = true, description = "")
		@JsonProperty("key")
		public String getKey() {
			return key;
		}
		public void setKey(String key) {
			this.key = key;
		}

		@Schema(required = true, description = "")
		@JsonProperty("value")
		public String getValue() {
			return value;
		}
		public void setValue(String value) {
			this.value = value;
		}
	}

}
