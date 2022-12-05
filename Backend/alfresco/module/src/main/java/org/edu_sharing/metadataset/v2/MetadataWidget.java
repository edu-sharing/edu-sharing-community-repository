package org.edu_sharing.metadataset.v2;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

public class MetadataWidget extends MetadataTranslatable{

	private Map<String, MetadataKey> valuesAsMapCache;
	private String configuration;

    public enum Required{
		mandatory,
		mandatoryForPublish,
		recommended,
		optional,
		ignore
	}
	public enum IdRelation{
		graphql,
	}
	public enum InteractionType {
		Input,
		None
	}
	public enum WidgetFilterMode {
		disabled,
		auto,
		always
	}
	public enum TextEscapingPolicy{
		// no escaping, strongly discouraged since it can allow XSS vulnerabilities if the data comes from untrusted sources
		none,
		// escape html but allow basic formatting and links (default)
		htmlBasic,
		// escape all data, only allow text
		all
	}
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
			"checkboxHorizontal",
			"checkboxVertical",
			"multivalueTree",
			"multivalueBadges",
			"multivalueFixedBadges",
			"multivalueSuggestBadges",
			"multivalueGroup",
			"multioption",
			"multivalueCombined"
	};

	private Map<IdRelation, String> ids = new HashMap<>();
	private String id,type,caption,bottomCaption,icon,
	placeholder,defaultvalue,template,
	suggestionSource,suggestionQuery,suggestDisplayProperty,unit,format,
	valuespaceSort="default";
	private Integer min,max,defaultMin,defaultMax,step;
	private boolean extended,allowempty,valuespaceClient=true,hideIfEmpty,inherit=true;
	private Required required = Required.optional;
	private List<MetadataKey> values;
	private List<Subwidget> subwidgets;
	private int maxlength;
	private TextEscapingPolicy textEscapingPolicy = TextEscapingPolicy.htmlBasic;
	private InteractionType interactionType = InteractionType.Input;
	private WidgetFilterMode filterMode = WidgetFilterMode.disabled;
	/**
	 * hint for the client if this widget creates a link to the search
	 * so e.g. if you click a keyword, you can be directed to the search with this keyword as filter
	 */
	private boolean searchable;

	private MetadataCondition condition;
	private String link;
	private String suggestionReceiver;

	public void setSuggestionReceiver(String suggestionReceiver) {
		this.suggestionReceiver = suggestionReceiver;
	}

	public String getSuggestionReceiver() {
		return suggestionReceiver;
	}

	public void setLink(String link) {
		this.link = link;
	}

	public String getLink() {
		return link;
	}



	public void setSearchable(boolean searchable) {
		this.searchable = searchable;
	}
	public boolean isSearchable() {
		return searchable;
	}
	public String getSuggestionQuery() {
		return suggestionQuery;
	}
	public void setSuggestionQuery(String suggestionQuery) {
		this.suggestionQuery = suggestionQuery;
	}
	public void setSuggestDisplayProperty(String suggestDisplayProperty) { this.suggestDisplayProperty = suggestDisplayProperty; }
	public String getSuggestDisplayProperty() { return suggestDisplayProperty; }

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
	public MetadataCondition getCondition() {
		return condition;
	}
	public void setCondition(MetadataCondition condition) {
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
	public Map<IdRelation, String> getIds() {
		return ids;
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
	public Required isRequired() {
		return required;
	}
	public void setRequired(Required required) {
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
	public void setValuespaceSort(String valuespaceSort) {
		this.valuespaceSort = valuespaceSort;
	}

	public String getValuespaceSort() {
		return valuespaceSort;
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


	public void setMaxlength(int maxlength) {
		this.maxlength = maxlength;
	}

	public int getMaxlength() {
		return maxlength;
	}

	public void setTextEscapingPolicy(TextEscapingPolicy textEscapingPolicy) {
		this.textEscapingPolicy = textEscapingPolicy;
	}

	public TextEscapingPolicy getTextEscapingPolicy() {
		return textEscapingPolicy;
	}
	public void setConfiguration(String configuration) {
		this.configuration = configuration;
	}

	public String getConfiguration() {
		return configuration;
	}

	public void setInteractionType(InteractionType interactionType) {
		this.interactionType = interactionType;
	}

	public InteractionType getInteractionType() {
		return interactionType;
	}

	public WidgetFilterMode getFilterMode() {
		return filterMode;
	}

	public void setFilterMode(WidgetFilterMode filterMode) {
		this.filterMode = filterMode;
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
		if(values==null) {
			return map;
		}
		if(valuesAsMapCache != null) {
			return valuesAsMapCache;
		}
		for(MetadataKey value : values){
			map.put(value.getKey(), value);
			if(value.getAlternativeKeys() != null) {
				value.getAlternativeKeys().forEach(
						(key) -> map.put(key, value)
				);
			}
		}
		valuesAsMapCache = map;
		return map;
	}
	public Map<String, Collection<MetadataKey.MetadataKeyRelated>> getValuespaceMappingByRelation(MetadataKey.MetadataKeyRelated.Relation relation) {
		Map<String, Collection<MetadataKey.MetadataKeyRelated>> map=new HashMap<>();
		for(MetadataKey value : values){
			map.put(value.getKey(), value.getRelated().stream().filter(r -> r.getRelation().equals(relation)).collect(Collectors.toList()));
		}
		return map;
	}
}
