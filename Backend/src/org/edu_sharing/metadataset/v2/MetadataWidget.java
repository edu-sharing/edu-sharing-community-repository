package org.edu_sharing.metadataset.v2;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.edu_sharing.metadataset.v2.MetadataWidget.Condition.CONDITION_TYPE;
import org.edu_sharing.service.nodeservice.NodeServiceImpl;
import org.edu_sharing.service.toolpermission.ToolPermissionServiceFactory;

import com.google.gwt.user.client.ui.WidgetCollection;

public class MetadataWidget extends MetadataTranslatable{
	public static class Subwidget implements Serializable {
		private String id;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

	}
	private static String[] MULTIVALUE_WIDGETS=new String[]{
			"vcard",
			"multivalueTree",
			"multivalueBadges",
			"multivalueFixedBadges",
			"multivalueSuggestBadges",
			"multivalueGroup",
			"multioption"
	};

	public static class Condition implements Serializable{
		public Condition(String value, CONDITION_TYPE type, boolean negate) {
			this.value = value;
			this.type = type;
			this.negate = negate;
		}
		public static enum CONDITION_TYPE{
			PROPERTY,
			TOOLPERMISSION
		};
		private String value;
		private CONDITION_TYPE type;
		private boolean negate;
		public String getValue() {
			return value;
		}
		public void setValue(String value) {
			this.value = value;
		}
		public CONDITION_TYPE getType() {
			return type;
		}
		public void setType(CONDITION_TYPE type) {
			this.type = type;
		}
		public boolean isNegate() {
			return negate;
		}
		public void setNegate(boolean negate) {
			this.negate = negate;
		}

	}
	private String id,type,caption,bottomCaption,icon,
					placeholder,defaultvalue,template,
					suggestionSource,suggestionQuery,unit,format;
	private Integer min,max,defaultMin,defaultMax,step;
	private boolean required,extended,allowempty,valuespaceClient=true,hideIfEmpty,inherit=true;
	private List<MetadataKey> values;
	private List<Subwidget> subwidgets;

	
	private Condition condition;
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
	public Condition getCondition() {
		return condition;
	}
	public void setCondition(Condition condition) {
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
	public List<Subwidget> getSubwidgets() {
		return subwidgets;
	}
	public void setSubwidgets(List<Subwidget> subwidgets) {
		this.subwidgets = subwidgets;
	}

	public boolean isInherit() {
		return inherit;
	}

	public void setInherit(boolean inherit) {
		this.inherit = inherit;
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
	
	//transient Logger logger = Logger.getLogger(MetadataWidget.class);
	/** resolves this widget's condition
	 * only works for condition type TOOLPERMISSION
	 * @return
	 */
	public boolean isConditionTrue() {
		Condition condition = getCondition();
		if(getCondition()==null)
			return true;
		if(Condition.CONDITION_TYPE.TOOLPERMISSION.equals(condition.getType())){
			boolean result=ToolPermissionServiceFactory.getInstance().hasToolPermission(condition.getValue());
			return result!=condition.isNegate();
		}
		//logger.info("skipping condition type "+condition.getType()+" for widget "+getId()+" since it's not supported in backend");
		return true;
	}
	
}
