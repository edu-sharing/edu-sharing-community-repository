/**
 *
 *  
 * 
 * 
 *	
 *
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 *
 */
package org.edu_sharing.repository.client.rpc.metadataset;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.edu_sharing.repository.client.tools.CCConstants;

/**
 * @author rudolph
 *
 */
public class MetadataSetBaseProperty implements com.google.gwt.user.client.rpc.IsSerializable{
	
	
	String type;
	String name;
	String widget;
	
	String formlength;
	String formheight;
	
	String copyfrom;
	
	MetadataSetValue label;
	
	MetadataSetValue labelHint;

	List<MetadataSetValueKatalog> valuespace;
	
	String valuespaceProvider = null;
	
	MetadataSetValue widgetTitle = null;
	
	String[] defaultValues = null;
	
	Integer id = null;
	
	//allow multiple virtual props that don't got an entry in the model part of an metadataset 
	Boolean multiple = null;
	
	
	HashMap<String,String> params = new HashMap<String,String>();
	
	
	String styleName = null;
	
	String styleNameLabel = null;
	
	/**
	 * html 5 placeholder
	 */
	MetadataSetValue placeHolder = null;
	
	
	Boolean toggle = false;
	
	/**
	 * html layout for the widget
	 */
	String layout = null;
	
	
	public static final String WIDGET_LISTBOX = "listbox";
	
	public static final String WIDGET_TEXTAREA = "textarea";
	
	public static final String WIDGET_TEXTFIELD = "textfield";
	
	public static final String WIDGET_TAXON = "taxon";
	
	public static final String WIDGET_TAXON_SIMPLE = "taxonsimple";
	
	public static final String WIDGET_CATALOG_COMPONENT = "catalogcomponent";
	
	public static final String WIDGET_POINTS = "points";
	
	public static final String WIDGET_DURATION = "duration";
	
	public static final String WIDGET_PERIOD = "period";
	
	public static final String WIDGET_VCARD = "vcard";
	
	public static final String WIDGET_VCARD_SIMPLE = "vcardsimple";
	
	public static final String WIDGET_DATEPICKER = "datepicker";
	
	public static final String WIDGET_CHECKBOX = "checkbox";
	
	public static final String WIDGET_SEARCHUSER = "searchuser";
	
	public static final String WIDGET_SEARCHCATEGORIES = "searchcategories";
	
	public static final String WIDGET_TAGCLOUD = "tagcloud";
	
	public static final String WIDGET_FACETTE = "facette";
	
	public static final String WIDGET_MULTIVALUE_SUGGESTBOX = "multivaluesuggestbox";
	
	public static final String WIDGET_MULTIVALUE_CHECKBOX = "multivaluecheckbox";
		
	public static final String WIDGET_GWTSUGGESTBOX = "gwtsuggestbox";
	
	public static final String WIDGET_MULTIVALUE_TEXTBOX = "multivaluetextbox";
	
	public static final String WIDGET_MULTIVALUE_GOOGLE_TEXTBOX = "multivaluegoogletextbox";
	
	public static final String WIDGET_SEARCH_SUGGESTBOX = "searchsuggestbox";
	
	public static final String WIDGET_CATEGORY_SUGGESTBOX = "categorysuggestbox";
	
	public static final String WIDGET_SUGGESTLISTBOX = "suggestlistbox";
	
	public static final String WIDGET_SUGGESTBOX_BIGDATA = "suggestboxbigdata";
	//allows free values
	public static final String WIDGET_SUGGESTBOX_BIGDATA_FREE = "suggestboxbigdatafree";
	
	public static final String WIDGET_MULTIVALUE_SUGGESTBOXLISTBOX_BIGDATA = "multivaluesuggestlistboxbigdata";
	
	public static final String WIDGET_MULTIVALUE_SUGGESTBOXLISTBOX_BIGDATA_FREE = "multivaluesuggestlistboxbigdatafree";
	
	/**
	 * the same as WIDGET_MULTIVALUE_TEXTBOX but you can put more values by pressing enter
	 */
	public static final String WIDGET_MULTIVALUE_TEXTBOX_FORM = "multivaluetextboxform";
	
	/**
	 * can be used as formfield which suggests values of a configured search
	 */
	public static final String WIDGET_MULTIVALUE_DYNAMIC_SUGGESTBOX = "multivaluedynamicsuggestbox";
	
	public static final String WIDGET_MULTIVALUE_SUGGESTBOXLISTBOX = "multivaluesuggestlistbox";
	
	public static final String WIDGET_NOWIDGET = "nowidget";
	
	public static final String WIDGET_TEXTFIELD_DISABLED = "textfielddisabled";
	
	public static final String WIDGET_HIDDEN = "hidden";
	
	public static final String WIDGET_LABEL = "label";
	
	public static final String WIDGET_CONTRIBUTER = "contributer";
	
	public static final String WIDGET_PREVIEW = "preview";
	
	public static final String WIDGET_FILE_LINK = "file_link";
	
	public static final String WIDGET_VERSION = "version";
	
	public static final String WIDGET_ALFRESCOCATEGORIES = "alfrescocategories";
	
	public static final String WIDGET_BUTTON = "button";
	
	public static final String WIDGET_NAME = "name";
	
	/**
	 * can be used to safe a build a path containing values of many widgets
	 */
	public static final String WIDGET_CONTEXT = "context";
	
	public static final String WIDGET_METADATA_PRESETTING = "metadatapresetting";
	
	public static final String WIDGET_JSONLIST = "jsonlist";
	
	
	/**
	 ************************* 
	 * Property Params
	 *************************
	 */
	
	/**
	 * for category widget: count an substring of the value
	 * for example 
	 * 
	 * 200 is "foreign languages"
	 * 20001 is "english"
	 * 
	 * if we want to count all foreign languages we have to count all values that start width "200"
	 * so we set countprops_substring = 3
	 */
	public static final String PARAM_COUNTPROPS_SUBSTRING = "countprops_substring";
	
	
	
	/**
	 * param to get the suggest value by concating the key and the value
	 * this is usefull if you got duplicates in value list i.e.:
	 * eaf catalog: the keys 120 and 280 both got the caption "Deutsch" 
	 * 
	 * default is false
	 */
	public static final String PARAM_MULTIVALUE_SUGGESTBOX_CONCATE_KEYVALUE = "multivalue_suggestbox_concate_keyvalue";
	
	public static final String PARAM_MULTIVALUE_SUGGESTBOX_ALLOW_NONCATALOG_VALUES = "multivalue_suggestbox_allow_non_catalog_values";
	
	
	/**
	 * the properties that are used to fill the suggestbox
	 */
	public static final String PARAM_SEARCH_SUGGESTBOX_PROPERTIES ="search_suggestbox_properties";
	
	
	/**
	 * param to enable or disable the blank splitting for MultiValueTextBox
	 */
	public static final String PARAM_MULTIVALUETEXTBOX_SPLITBLANKS = "multivaluetextbox_splitblanks";
	
	/**
	 * {@link WIDGET_ALFRESCOCATEGORIES} 
	 */
	public static final String PARAM_ALFRESCO_CATEGORIES_START = "start";
	
	public static final String PARAM_ALFRESCO_CATEGORIES_LEVEL = "level";
	
	public static final String PARAM_ALFRESCO_CATEGORIES_LABELS = "labels";
	
	
	/**
	 * {@link WIDGET_CONTEXT}
	 */
	//the formelents to get values for building path
	public static final String PARAM_CONTEXT_FORM_REFERENCES = "form_references";
	
	
	
	public static final String PARAM_SUGGESTBOX_DAO = "suggestbox_dao";
	
	
	/**
	 * {@link WIDGET_METADATA_PRESETTING}
	 */
	
	public static final String PARAM_METADATA_PRESETTING_PROPERTIES = "metadata_presetting_properties";
	
	
	public static final String PARAM_VERSION_CHECKBOX_CHECKED = "checked";
	
	
	/**
	 * ******************************************
	 * reserved PropertyNames
	 * ******************************************
	 */
	
	/**
	 * for federated repositories use the folling const as property name and WIDGET_MULTIVALUE_SUGGESTBOX as widget
	 */
	public static final String PROPERTY_NAME_CONSTANT_EDU_SHARING_REPOSITORIES = "EDU_SHARING_REPOSITORIES";
	
	/**
	 * the serachword const
	 */
	public static final String PROPERTY_NAME_CONSTANT_searchword = "searchword";
	
	/**
	 *  the path constant
	 */
	public static final String PROPERTY_NAME_CONSTANT_path = "path";
	
	/**
	 * the invited constant
	 */
	public static final String PROPERTY_NAME_CONSTANT_invited = "invited";

	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * @return the widget
	 */
	public String getWidget() {
		return widget;
	}

	/**
	 * @param widget the widget to set
	 */
	public void setWidget(String widget) {
		this.widget = widget;
	}

	/**
	 * @return the valuespace
	 */
	public List<MetadataSetValueKatalog> getValuespace() {
		return valuespace;
	}

	/**
	 * @param valuespace the valuespace to set
	 */
	public void setValuespace(List<MetadataSetValueKatalog> valuespace) {
		this.valuespace = valuespace;
	}
	
	
	/**
	 * @return the formlength
	 */
	public String getFormlength() {
		return formlength;
	}

	/**
	 * @param formlength the formlength to set
	 */
	public void setFormlength(String formlength) {
		this.formlength = formlength;
	}

	/**
	 * @return the label
	 */
	public MetadataSetValue getLabel() {
		return label;
	}

	/**
	 * @param label the label to set
	 */
	public void setLabel(MetadataSetValue label) {
		this.label = label;
	}

	/**
	 * @return the formheight
	 */
	public String getFormheight() {
		return formheight;
	}

	/**
	 * @param formheight the formheight to set
	 */
	public void setFormheight(String formheight) {
		this.formheight = formheight;
	}

	/**
	 * @return the copyfrom
	 */
	public String getCopyfrom() {
		return copyfrom;
	}

	/**
	 * @param copyfrom the copyfrom to set
	 */
	public void setCopyfrom(String copyfrom) {
		this.copyfrom = copyfrom;
	}
	
	/**
	 * @return the defaultValue
	 */
	public String[] getDefaultValues() {
		return defaultValues;
	}

	/**
	 * @param defaultValue the defaultValue to set
	 */
	public void setDefaultValues(String[] defaultValues) {
		this.defaultValues = defaultValues;
	}
	
	public String getValueOfValueSpace(String key , String locale){
		if(this.valuespace != null && this.valuespace.size() > 0){
			for(MetadataSetValueKatalog mdsv : this.valuespace){
				if(mdsv.getKey().equals(key) ){
					
					String tmpVal = null;
					if(mdsv.i18n != null){
						tmpVal = mdsv.i18n.get(locale);
						if(tmpVal == null) tmpVal = mdsv.i18n.get(CCConstants.defaultLocale);
						
					}
					
					if(tmpVal == null && mdsv.getCaption() != null && !mdsv.getCaption().trim().equals("")){
						tmpVal = mdsv.getCaption();
					}
					
					
					if(tmpVal != null){
						return tmpVal;
					}
					
				}
			}
		}
		return key;
	}
	
	public MetadataSetValueKatalog getMetadataSetValueKatalog(String key){
		if(this.valuespace != null && this.valuespace.size() > 0){
			for(MetadataSetValueKatalog mdsv : this.valuespace){
				if(mdsv.getKey().equals(key) ){
					return mdsv;
				}
			}
		}
		return null;
	}
	
	public MetadataSetValue getWidgetTitle() {
		return widgetTitle;
	}
	
	public void setWidgetTitle(MetadataSetValue widgetTitle) {
		this.widgetTitle = widgetTitle;
	}
	
	public Set<String> getParamKeys() {		
		return params.keySet();
	}
	
	public String getParam(String key){
		return params.get(key);
	}
	
	public void setParam(String key, String value){
		params.put(key, value);
	}
	
	public Boolean getMultiple() {
		if(multiple == null) return false;
		return multiple;
	}
	
	public void setMultiple(Boolean multiple) {
		this.multiple = multiple;
	}
	
	public String getStyleName() {
		return styleName;
	}

	public void setStyleName(String styleName) {
		this.styleName = styleName;
	}

	public String getStyleNameLabel() {
		return styleNameLabel;
	}

	public void setStyleNameLabel(String styleNameLabel) {
		this.styleNameLabel = styleNameLabel;
	}
	
	public void setValuespaceProvider(String valuespaceProvider) {
		this.valuespaceProvider = valuespaceProvider;
	}
	
	public String getValuespaceProvider() {
		return valuespaceProvider;
	}
	
	public void setPlaceHolder(MetadataSetValue placeHolder) {
		this.placeHolder = placeHolder;
	}
	
	public MetadataSetValue getPlaceHolder() {
		return placeHolder;
	}
	
	public void setLabelHint(MetadataSetValue labelHint) {
		this.labelHint = labelHint;
	}
	
	public MetadataSetValue getLabelHint() {
		return labelHint;
	}
	
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}
	
	public Boolean getToggle() {
		return toggle;
	}

	public void setToggle(String toggle) {
		this.toggle = new Boolean(toggle);
	}
}
