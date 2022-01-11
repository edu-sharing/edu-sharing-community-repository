package org.edu_sharing.restservices.shared;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

@ApiModel(description = "")
public class MdsView {

	private String id = null;
	private List<MdsViewProperty> properties = null;
	
	@ApiModelProperty(required = true, value = "")
	@JsonProperty("id")
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	@ApiModelProperty(required = true, value = "")
	@JsonProperty("properties")	
	public List<MdsViewProperty> getProperties() {
		return properties;
	}
	public void setProperties(List<MdsViewProperty> properties) {
		this.properties = properties;
	}
	

	@ApiModel(description = "")
	public static class MdsViewProperty {

		private String name = null;
		private String label = null;
		private String labelHint = null;
		private String formHeight = null;
		private String formLength = null;
		private String widget = null;
		private String widgetTitle = null;
		private List<String> copyFrom = null;
		private List<MdsViewPropertyParameter> parameters = null;
		private List<MdsViewPropertyValue> values = null;
		private List<String> defaultValues = null;
		private Boolean multiple = null;
		private String placeHolder = null;
		private String styleName = null;
		private String styleNameLabel = null;
		private String type = null;
		
		@ApiModelProperty(required = true, value = "")
		@JsonProperty("name")
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@ApiModelProperty(required = true, value = "")
		@JsonProperty("label")
		public String getLabel() {
			return label;
		}

		public void setLabel(String label) {
			this.label = label;
		}

		@ApiModelProperty(required = true, value = "")
		@JsonProperty("labelHint")
		public String getLabelHint() {
			return labelHint;
		}

		public void setLabelHint(String labelHint) {
			this.labelHint = labelHint;
		}

		@ApiModelProperty(required = true, value = "")
		@JsonProperty("formHeight")
		public String getFormHeight() {
			return formHeight;
		}

		public void setFormHeight(String formHeight) {
			this.formHeight = formHeight;
		}

		@ApiModelProperty(required = true, value = "")
		@JsonProperty("formLength")
		public String getFormLength() {
			return formLength;
		}

		public void setFormLength(String formLength) {
			this.formLength = formLength;
		}

		@ApiModelProperty(required = true, value = "")
		@JsonProperty("widget")
		public String getWidget() {
			return widget;
		}

		public void setWidget(String widget) {
			this.widget = widget;
		}

		@ApiModelProperty(required = true, value = "")
		@JsonProperty("widgetTitle")
		public String getWidgetTitle() {
			return widgetTitle;
		}

		public void setWidgetTitle(String widgetTitle) {
			this.widgetTitle = widgetTitle;
		}

		@ApiModelProperty(required = true, value = "")
		@JsonProperty("copyFrom")
		public List<String> getCopyFrom() {
			return copyFrom;
		}

		public void setCopyFrom(List<String> copyFrom) {
			this.copyFrom = copyFrom;
		}

		@ApiModelProperty(required = true, value = "")
		@JsonProperty("parameters")
		public List<MdsViewPropertyParameter> getParameters() {
			return parameters;
		}

		public void setParameters(List<MdsViewPropertyParameter> parameters) {
			this.parameters = parameters;
		}

		@ApiModelProperty(required = true, value = "")
		@JsonProperty("values")
		public List<MdsViewPropertyValue> getValues() {
			return values;
		}
		public void setValues(List<MdsViewPropertyValue> values) {
			this.values = values;
		}

		@ApiModelProperty(required = true, value = "")
		@JsonProperty("defaultValues")
		public List<String> getDefaultValues() {
			return defaultValues;
		}

		public void setDefaultValues(List<String> defaultValues) {
			this.defaultValues = defaultValues;
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
		@JsonProperty("placeHolder")
		public String getPlaceHolder() {
			return placeHolder;
		}

		public void setPlaceHolder(String placeHolder) {
			this.placeHolder = placeHolder;
		}

		@ApiModelProperty(required = true, value = "")
		@JsonProperty("styleName")		
		public String getStyleName() {
			return styleName;
		}

		public void setStyleName(String styleName) {
			this.styleName = styleName;
		}

		@ApiModelProperty(required = true, value = "")
		@JsonProperty("styleNameLabel")
		public String getStyleNameLabel() {
			return styleNameLabel;
		}

		public void setStyleNameLabel(String styleNameLabel) {
			this.styleNameLabel = styleNameLabel;
		}

		@ApiModelProperty(required = true, value = "")
		@JsonProperty("type")
		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}
	}


	@ApiModel(description = "")
	public static class MdsViewPropertyValue {

		private String key = null;
		private String value = null;
		
		@ApiModelProperty(required = true, value = "")
		@JsonProperty("key")
		public String getKey() {
			return key;
		}
		public void setKey(String key) {
			this.key = key;
		}

		@ApiModelProperty(required = true, value = "")
		@JsonProperty("value")
		public String getValue() {
			return value;
		}
		public void setValue(String value) {
			this.value = value;
		}
	}
	
	@ApiModel(description = "")
	public static class MdsViewPropertyParameter {

		private String name = null;
		private String value = null;
		
		@ApiModelProperty(required = true, value = "")
		@JsonProperty("name")
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}

		@ApiModelProperty(required = true, value = "")
		@JsonProperty("value")
		public String getValue() {
			return value;
		}
		public void setValue(String value) {
			this.value = value;
		}
	}

	
}
