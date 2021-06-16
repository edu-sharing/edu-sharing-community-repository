package org.edu_sharing.restservices.mds.v1.model;

import java.util.ArrayList;
import java.util.List;

import org.edu_sharing.metadataset.v2.MetadataKey;
import org.edu_sharing.metadataset.v2.MetadataReaderV2;
import org.edu_sharing.metadataset.v2.MetadataWidget;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
@ApiModel(description = "")
public class WidgetV2 {

	public static class Condition{
		private String value,type;
		private boolean negate;
		private boolean dynamic;
		private String pattern;

		@JsonProperty
		public String getValue() {
			return value;
		}
		public void setValue(String value) {
			this.value = value;
		}
		@JsonProperty
		public String getType() {
			return type;
		}
		public void setType(String type) {
			this.type = type;
		}
		@JsonProperty
		public boolean isNegate() {
			return negate;
		}
		public void setNegate(boolean negate) {
			this.negate = negate;
		}

		@JsonProperty
		public void setDynamic(boolean dynamic) {
			this.dynamic = dynamic;
		}

		public boolean getDynamic() {
			return dynamic;
		}

		public void setPattern(String pattern) {
			this.pattern = pattern;
		}

		public String getPattern() {
			return pattern;
		}
	}
	public static class Subwidget{
    			private String id;

    			public Subwidget(org.edu_sharing.metadataset.v2.MetadataWidget.Subwidget key) {
    				this.id = key.getId();
    			}

    			public String getId() {
    				return id;
    			}

    			public void setId(String id) {
    				this.id = id;
    			}

    }
		private String id,caption,bottomCaption,icon,type,link,template;
		private boolean hasValues;
		private List<ValueV2> values;
		private List<Subwidget> subwidgets;
		private String placeholder;
		private String unit;
		private Integer min;
		private Integer max;
		private Integer defaultMin;
		private Integer defaultMax;
		private Integer step;
		private boolean isExtended;
		private MetadataWidget.Required isRequired;
		private boolean allowempty;
		private String defaultvalue;
		private boolean isSearchable;
		private Condition condition;
		private int maxlength;

	public WidgetV2(){}
		public WidgetV2(MetadataWidget widget) {
			this.id=widget.getId();		
			this.caption=widget.getCaption();
			this.bottomCaption=widget.getBottomCaption();
			this.icon=widget.getIcon();
			this.type=widget.getType();
			this.link=widget.getLink();
			this.defaultvalue=widget.getDefaultvalue();
			this.placeholder=widget.getPlaceholder();
			this.maxlength=widget.getMaxlength();
			this.unit=widget.getUnit();
			this.min=widget.getMin();
			this.max=widget.getMax();
			this.defaultMin=widget.getDefaultMin();
			this.defaultMax=widget.getDefaultMax();
			this.step=widget.getStep();
			this.template=widget.getTemplate();
			this.isExtended=widget.isExtended();
			this.isRequired=widget.isRequired();
			this.allowempty=widget.isAllowempty();
			this.isSearchable=widget.isSearchable();
			if(widget.getCondition()!=null) {
				this.condition=new Condition();
				this.condition.setValue(widget.getCondition().getValue());
				this.condition.setType(widget.getCondition().getType().name());
				this.condition.setNegate(widget.getCondition().isNegate());
				this.condition.setDynamic(widget.getCondition().isDynamic());
				this.condition.setPattern(widget.getCondition().getPattern());
			}
			if(widget.getValues()!=null){
				this.hasValues = true;
				if(widget.isValuespaceClient()) {
					 values = new ArrayList<ValueV2>();
					 for (MetadataKey key : widget.getValues()) {
						 values.add(new ValueV2(key));
					 }
				 }
			}
			if(widget.getSubwidgets()!=null){
				subwidgets=new ArrayList<Subwidget>();
				for(MetadataWidget.Subwidget key : widget.getSubwidgets()){
					subwidgets.add(new Subwidget(key));
				}
			}

		}
		@JsonProperty
		public Condition getCondition() {
			return condition;
		}
		public void setCondition(Condition condition) {
			this.condition = condition;
		}
		@JsonProperty("isSearchable")
		public boolean isSearchable() {
			return isSearchable;
		}
		public void setSearchable(boolean searchable) {
		this.isSearchable = searchable;
	}
		@JsonProperty("bottomCaption")
		public String getBottomCaption() {
			return bottomCaption;
		}
		public void setBottomCaption(String bottomCaption) {
			this.bottomCaption = bottomCaption;
		}
		@JsonProperty("defaultvalue")
		public String getDefaultvalue() {
			return defaultvalue;
		}
		public void setDefaultvalue(String defaultvalue) {
			this.defaultvalue = defaultvalue;
		}
		@JsonProperty("template")
		public String getTemplate() {
			return template;
		}
		public void setTemplate(String template) {
			this.template = template;
		}
		@JsonProperty("icon")
		public String getIcon() {
			return icon;
		}
		public void setIcon(String icon) {
			this.icon = icon;
		}
		@JsonProperty("placeholder")
		public String getPlaceholder() {
			return placeholder;
		}
		public void setPlaceholder(String placeholder) {
			this.placeholder = placeholder;
		}
		@JsonProperty("min")
		public Integer getMin() {
			return min;
		}
		public void setMin(Integer min) {
			this.min = min;
		}
		@JsonProperty("max")
		public Integer getMax() {
			return max;
		}
		public void setMax(Integer max) {
			this.max = max;
		}
		@JsonProperty("defaultMin")
		public Integer getDefaultMin() {
			return defaultMin;
		}
		public void setDefaultMin(Integer defaultMin) {
			this.defaultMin = defaultMin;
		}
		@JsonProperty("defaultMax")
		public Integer getDefaultMax() {
			return defaultMax;
		}
		public void setDefaultMax(Integer defaultMax) {
			this.defaultMax = defaultMax;
		}
		@JsonProperty("step")
		public Integer getStep() {
			return step;
		}
		public void setStep(Integer step) {
			this.step = step;
		}
		@JsonProperty("isExtended")
		public boolean isExtended() {
			return isExtended;
		}
		public void setExtended(boolean isExtended) {
			this.isExtended = isExtended;
		}
		@JsonProperty("allowempty")
		public boolean isAllowempty() {
			return allowempty;
		}
		public void setAllowempty(boolean allowempty) {
			this.allowempty = allowempty;
		}
		@JsonProperty("isRequired")
		public MetadataWidget.Required isRequired() {
			return isRequired;
		}
		public void setRequired(MetadataWidget.Required isRequired) {
			this.isRequired = isRequired;
		}
		@JsonProperty("values")
		public List<ValueV2> getValues() {
			return values;
		}
		public void setValues(List<ValueV2> values) {
			this.values = values;
		}

		@JsonProperty("hasValues")
		public boolean isHasValues() {
			return hasValues;
		}

		public void setHasValues(boolean hasValues) {
			this.hasValues = hasValues;
		}

		@JsonProperty("caption")
		public String getCaption() {
			return caption;
		}
		public void setCaption(String caption) {
			this.caption = caption;
		}
		@JsonProperty("type")
		public String getType() {
			return type;
		}
		public void setType(String type) {
			this.type = type;
		}
		@JsonProperty
		public String getLink() {
			return link;
		}

		public void setLink(String link) {
			this.link = link;
		}
		@JsonProperty("id")
		public String getId() {
			return id;
		}
		public void setId(String id) {
			this.id = id;
		}
		@JsonProperty("unit")
		public String getUnit() {
			return unit;
		}
		public void setUnit(String unit) {
			this.unit = unit;
		}
		@JsonProperty
		public List<Subwidget> getSubwidgets() {
			return subwidgets;
		}
		public void setSubwidgets(List<Subwidget> subwidgets) {
			this.subwidgets = subwidgets;
		}
		@JsonProperty
		public int getMaxlength() {
			return maxlength;
		}

		public void setMaxlength(int maxlength) {
			this.maxlength = maxlength;
		}
}

