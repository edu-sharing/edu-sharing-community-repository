package org.edu_sharing.metadataset.v2;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MetadataWidget extends MetadataTranslatable{
	private static String[] MULTIVALUE_WIDGETS=new String[]{
			"vcard",
			"multivalueTree",
			"multivalueBadges",
			"multivalueFixedBadges",
			"multivalueSuggestBadges",
			"multioption"
	};
	private String id,type,caption,bottomCaption,icon,
					placeholder,defaultvalue,template,condition,
					suggestionSource,suggestionQuery,unit,format;
	private Integer min,max,defaultValue,defaultMin,defaultMax,step;
	private boolean required,extended,allowempty,valuespaceClient=true,hideIfEmpty;
	private List<MetadataKey> values;

	public String getSuggestionQuery() {
		return suggestionQuery;
	}
	public void setSuggestionQuery(String suggestionQuery) {
		this.suggestionQuery = suggestionQuery;
	}
	public String getUnit() {
		return unit;
	}
	public void setUnit(String unit) {
		this.unit = unit;
	}
	public String getFormat() {
		return format;
	}
	public void setFormat(String format) {
		this.format = format;
	}
	public String getSuggestionSource() {
		return suggestionSource;
	}
	public void setSuggestionSource(String suggestionSource) {
		this.suggestionSource = suggestionSource;
	}
	public boolean isValuespaceClient() {
		return valuespaceClient;
	}
	public void setValuespaceClient(boolean valuespaceClient) {
		this.valuespaceClient = valuespaceClient;
	}
	public boolean isMultivalue(){
		return Arrays.asList(MULTIVALUE_WIDGETS).contains(type);
	}
	public String getCondition() {
		return condition;
	}
	public void setCondition(String condition) {
		this.condition = condition;
	}
	public String getTemplate() {
		return template;
	}
	public void setTemplate(String template) {
		this.template = template;
	}
	public boolean isExtended() {
		return extended;
	}
	public void setExtended(boolean extended) {
		this.extended = extended;
	}	
	public boolean isHideIfEmpty() {
		return hideIfEmpty;
	}
	public void setHideIfEmpty(boolean hideIfEmpty) {
		this.hideIfEmpty = hideIfEmpty;
	}
	public String getDefaultvalue() {
		return defaultvalue;
	}
	public void setDefaultvalue(String defaultvalue) {
		this.defaultvalue = defaultvalue;
	}
	public String getPlaceholder() {
		//return MetadataReaderV2.getTranslation(this,placeholder,locale);
		return placeholder;
	}
	public void setPlaceholder(String placeholder) {
		this.placeholder = placeholder;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public List<MetadataKey> getValues() {
		return values;
	}
	public Integer getMin() {
		return min;
	}
	public void setMin(Integer min) {
		this.min = min;
	}
	public Integer getMax() {
		return max;
	}
	public void setMax(Integer max) {
		this.max = max;
	}
	public Integer getDefaultValue() {
		return defaultValue;
	}
	public void setDefaultValue(Integer defaultValue) {
		this.defaultValue = defaultValue;
	}
	public Integer getDefaultMin() {
		return defaultMin;
	}
	public void setDefaultMin(Integer defaultMin) {
		this.defaultMin = defaultMin;
	}
	public Integer getDefaultMax() {
		return defaultMax;
	}
	public void setDefaultMax(Integer defaultMax) {
		this.defaultMax = defaultMax;
	}
	public String getCaption() {
		//return MetadataReaderV2.getTranslation(this,caption,locale);
		return caption;
	}
	public void setCaption(String caption) {
		this.caption = caption;
	}
	public boolean isRequired() {
		return required;
	}
	public void setRequired(boolean required) {
		this.required = required;
	}
	public void setValues(List<MetadataKey> values) {
		this.values = values;
	}
	public String getIcon() {
		return icon;
	}
	public void setIcon(String icon) {
		this.icon = icon;
	}
	public Integer getStep() {
		return step;
	}
	public void setStep(Integer step) {
		this.step = step;
	}
	
	public boolean isAllowempty() {
		return allowempty;
	}
	public void setAllowempty(boolean allowempty) {
		this.allowempty = allowempty;
	}
	
	public String getBottomCaption() {
		return bottomCaption;
	}
	public void setBottomCaption(String bottomCaption) {
		this.bottomCaption = bottomCaption;
	}
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof MetadataWidget){
			MetadataWidget other=(MetadataWidget)obj;
			if(other.id.equals(id)){
				return templateEquals(other) && conditionEquals(other);
			}
			return false;				
		}
		return super.equals(obj);
	}
	private boolean templateEquals(MetadataWidget other) {
		if(other.template==null)
			return template==null;
		return other.template.equals(template);
	}
	private boolean conditionEquals(MetadataWidget other) {
		if(other.condition==null)
			return condition==null;
		return other.condition.equals(condition);
	}
	public Map<String, MetadataKey> getValuesAsMap() {
		Map<String,MetadataKey> map=new HashMap<>();
		if(values==null)
			return map;
		for(MetadataKey value : values){
			map.put(value.getKey(), value);
		}
		return map;
	}
	
}
