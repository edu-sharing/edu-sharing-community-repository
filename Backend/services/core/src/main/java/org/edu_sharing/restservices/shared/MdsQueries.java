package org.edu_sharing.restservices.shared;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

@ApiModel(description = "")
public class MdsQueries {

	private String baseQuery = null;
	
	private List<MdsQuery> queries= null;
	
	@ApiModelProperty(required = true, value = "")
	@JsonProperty("baseQuery")
	public String getBaseQuery() {
		return baseQuery;
	}
	public void setBaseQuery(String baseQuery) {
		this.baseQuery = baseQuery;
	}
	
	@ApiModelProperty(required = true, value = "")
	@JsonProperty("queries")	
	public List<MdsQuery> getQueries() {
		return queries;
	}
	public void setQueries(List<MdsQuery> queries) {
		this.queries = queries;
	}
	

	@ApiModel(description = "")
	public static class MdsQuery {
		
		private String criteriaboxid = null;
		private String handlerclass = null;
		private String join = null;
		private String label = null;
		private String layout = null;
		private List<MdsQueryProperty> properties = null;
		private String statement = null;
		private String stylename = null;
		private String widget = null;
		
		@ApiModelProperty(required = true, value = "")
		@JsonProperty("criteriaboxid")
		public String getCriteriaboxid() {
			return criteriaboxid;
		}
		public void setCriteriaboxid(String criteriaboxid) {
			this.criteriaboxid = criteriaboxid;
		}

		@ApiModelProperty(required = true, value = "")
		@JsonProperty("handlerclass")
		public String getHandlerclass() {
			return handlerclass;
		}
		public void setHandlerclass(String handlerclass) {
			this.handlerclass = handlerclass;
		}
		
		@ApiModelProperty(required = true, value = "")
		@JsonProperty("join")		
		public String getJoin() {
			return join;
		}
		public void setJoin(String join) {
			this.join = join;
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
		@JsonProperty("layout")
		public String getLayout() {
			return layout;
		}
		public void setLayout(String layout) {
			this.layout = layout;
		}
		
		@ApiModelProperty(required = true, value = "")
		@JsonProperty("properties")
		public List<MdsQueryProperty> getProperties() {
			return properties;
		}
		public void setProperties(List<MdsQueryProperty> properties) {
			this.properties = properties;
		}
		
		@ApiModelProperty(required = true, value = "")
		@JsonProperty("statement")		
		public String getStatement() {
			return statement;
		}
		public void setStatement(String statement) {
			this.statement = statement;
		}
		
		@ApiModelProperty(required = true, value = "")
		@JsonProperty("stylename")
		public String getStylename() {
			return stylename;
		}
		public void setStylename(String stylename) {
			this.stylename = stylename;
		}
		
		@ApiModelProperty(required = true, value = "")
		@JsonProperty("widget")
		public String getWidget() {
			return widget;
		}
		public void setWidget(String widget) {
			this.widget = widget;
		}
		
		
	}
	
	@ApiModel(description = "")
	public static class MdsQueryProperty {
		
		private String name = null;
		private String label = null;
		private String labelHint = null;
		private String formHeight = null;
		private String formLength = null;
		private String widget = null;
		private String widgetTitle = null;
		private List<String> copyFrom = null;
		private List<MdsQueryPropertyParameter> parameters = null;
		private List<MdsQueryPropertyValue> values = null;
		private List<String> defaultValues = null;
		private Boolean multiple = null;
		private String placeHolder = null;
		private String styleName = null;
		private String styleNameLabel = null;
		private String type = null;		
		private List<String> validators = null;
		
		private String statement = null;		
		private String multiplejoin = null;
		private Boolean toggle = false;		
		private String initByGetParam = null;
		
		
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
		@JsonProperty("validators")
		public List<String> getValidators() {
			return validators;
		}

		public void setValidators(List<String> validators) {
			this.validators = validators;
		}

		@ApiModelProperty(required = true, value = "")
		@JsonProperty("parameters")
		public List<MdsQueryPropertyParameter> getParameters() {
			return parameters;
		}

		public void setParameters(List<MdsQueryPropertyParameter> parameters) {
			this.parameters = parameters;
		}

		@ApiModelProperty(required = true, value = "")
		@JsonProperty("values")
		public List<MdsQueryPropertyValue> getValues() {
			return values;
		}
		public void setValues(List<MdsQueryPropertyValue> values) {
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

		@ApiModelProperty(required = true, value = "")
		@JsonProperty("statement")
		public String getStatement() {
			return statement;
		}

		public void setStatement(String statement) {
			this.statement = statement;
		}

		@ApiModelProperty(required = true, value = "")
		@JsonProperty("multipleJoin")		
		public String getMultiplejoin() {
			return multiplejoin;
		}

		public void setMultiplejoin(String multiplejoin) {
			this.multiplejoin = multiplejoin;
		}

		@ApiModelProperty(required = true, value = "")
		@JsonProperty("toogle")
		public Boolean getToggle() {
			return toggle;
		}

		public void setToggle(Boolean toggle) {
			this.toggle = toggle;
		}

		@ApiModelProperty(required = true, value = "")
		@JsonProperty("initByGetParam")		
		public String getInitByGetParam() {
			return initByGetParam;
		}

		public void setInitByGetParam(String initByGetParam) {
			this.initByGetParam = initByGetParam;
		}

	}
	
	@ApiModel(description = "")
	public static class MdsQueryPropertyParameter {

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

	@ApiModel(description = "")
	public static class MdsQueryPropertyValue {

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
	
}
