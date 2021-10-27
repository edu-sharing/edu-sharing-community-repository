package org.edu_sharing.restservices.shared;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

@Schema(description = "")
public class MdsQueries {

	private String baseQuery = null;
	
	private List<MdsQuery> queries= null;
	
	@Schema(required = true, description = "")
	@JsonProperty("baseQuery")
	public String getBaseQuery() {
		return baseQuery;
	}
	public void setBaseQuery(String baseQuery) {
		this.baseQuery = baseQuery;
	}
	
	@Schema(required = true, description = "")
	@JsonProperty("queries")	
	public List<MdsQuery> getQueries() {
		return queries;
	}
	public void setQueries(List<MdsQuery> queries) {
		this.queries = queries;
	}
	

	@Schema(description = "")
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
		
		@Schema(required = true, description = "")
		@JsonProperty("criteriaboxid")
		public String getCriteriaboxid() {
			return criteriaboxid;
		}
		public void setCriteriaboxid(String criteriaboxid) {
			this.criteriaboxid = criteriaboxid;
		}

		@Schema(required = true, description = "")
		@JsonProperty("handlerclass")
		public String getHandlerclass() {
			return handlerclass;
		}
		public void setHandlerclass(String handlerclass) {
			this.handlerclass = handlerclass;
		}
		
		@Schema(required = true, description = "")
		@JsonProperty("join")		
		public String getJoin() {
			return join;
		}
		public void setJoin(String join) {
			this.join = join;
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
		@JsonProperty("layout")
		public String getLayout() {
			return layout;
		}
		public void setLayout(String layout) {
			this.layout = layout;
		}
		
		@Schema(required = true, description = "")
		@JsonProperty("properties")
		public List<MdsQueryProperty> getProperties() {
			return properties;
		}
		public void setProperties(List<MdsQueryProperty> properties) {
			this.properties = properties;
		}
		
		@Schema(required = true, description = "")
		@JsonProperty("statement")		
		public String getStatement() {
			return statement;
		}
		public void setStatement(String statement) {
			this.statement = statement;
		}
		
		@Schema(required = true, description = "")
		@JsonProperty("stylename")
		public String getStylename() {
			return stylename;
		}
		public void setStylename(String stylename) {
			this.stylename = stylename;
		}
		
		@Schema(required = true, description = "")
		@JsonProperty("widget")
		public String getWidget() {
			return widget;
		}
		public void setWidget(String widget) {
			this.widget = widget;
		}
		
		
	}
	
	@Schema(description = "")
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
		@JsonProperty("validators")
		public List<String> getValidators() {
			return validators;
		}

		public void setValidators(List<String> validators) {
			this.validators = validators;
		}

		@Schema(required = true, description = "")
		@JsonProperty("parameters")
		public List<MdsQueryPropertyParameter> getParameters() {
			return parameters;
		}

		public void setParameters(List<MdsQueryPropertyParameter> parameters) {
			this.parameters = parameters;
		}

		@Schema(required = true, description = "")
		@JsonProperty("values")
		public List<MdsQueryPropertyValue> getValues() {
			return values;
		}
		public void setValues(List<MdsQueryPropertyValue> values) {
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

		@Schema(required = true, description = "")
		@JsonProperty("statement")
		public String getStatement() {
			return statement;
		}

		public void setStatement(String statement) {
			this.statement = statement;
		}

		@Schema(required = true, description = "")
		@JsonProperty("multipleJoin")		
		public String getMultiplejoin() {
			return multiplejoin;
		}

		public void setMultiplejoin(String multiplejoin) {
			this.multiplejoin = multiplejoin;
		}

		@Schema(required = true, description = "")
		@JsonProperty("toogle")
		public Boolean getToggle() {
			return toggle;
		}

		public void setToggle(Boolean toggle) {
			this.toggle = toggle;
		}

		@Schema(required = true, description = "")
		@JsonProperty("initByGetParam")		
		public String getInitByGetParam() {
			return initByGetParam;
		}

		public void setInitByGetParam(String initByGetParam) {
			this.initByGetParam = initByGetParam;
		}

	}
	
	@Schema(description = "")
	public static class MdsQueryPropertyParameter {

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
	public static class MdsQueryPropertyValue {

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
