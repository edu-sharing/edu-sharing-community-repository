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

import java.util.HashMap;
import java.util.List;

/**
 * @author rudolph
 */
public class MetadataSetFormsPanel implements com.google.gwt.user.client.rpc.IsSerializable, HasLayout {
	
	String name;
	Boolean oncreate;
	Boolean onupdate;
	Boolean onmultiupload=false;
	Boolean onsingleupload=true;
	

	List<MetadataSetFormsProperty> properties;
	String order;
	
	MetadataSetValue label;
	
	
	MetadataSetFormsForm parent;
	
	Boolean multiupload = false;
	
	String styleName;
	
	String layout;
	
	
	public MetadataSetFormsPanel() {
	}

	/**
	 * @return the label
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param label the label to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the properties
	 */
	public List<MetadataSetFormsProperty> getProperties() {
		return properties;
	}
	
	public MetadataSetFormsProperty getProperty(String propName){
		for(MetadataSetFormsProperty prop : getProperties()){
			if(prop.getName().equals(propName)){
				return prop;
			}
		}
		
		return null;
	}

	/**
	 * @param properties the properties to set
	 */
	public void setProperties(List<MetadataSetFormsProperty> properties) {
		this.properties = properties;
	}

	/**
	 * @return the order
	 */
	public String getOrder() {
		return order;
	}

	/**
	 * @param order the order to set
	 */
	public void setOrder(String order) {
		this.order = order;
	}

	/**
	 * @return the parent
	 */
	public MetadataSetFormsForm getParent() {
		return parent;
	}

	/**
	 * @param parent the parent to set
	 */
	public void setParent(MetadataSetFormsForm parent) {
		this.parent = parent;
	}

	/**
	 * @return the label
	 */
	public MetadataSetValue getLabel() {
		return label;
	}
	
	
	public String getLabel(String locale){
		
		MetadataSetValue labelMdsV = getLabel();
		String label = null;
		if(labelMdsV != null){
			HashMap<String, String>  i18nLabel = labelMdsV.getI18n();
			if(i18nLabel != null && i18nLabel.size() > 0){
				label = i18nLabel.get(locale);
			}else{
				label = labelMdsV.getKey();
			}
		}
		label = (label == null) ? getName() : label;
		return label;
	}

	/**
	 * @param label the label to set
	 */
	public void setLabel(MetadataSetValue label) {
		this.label = label;
	}

	/**
	 * @return the oncreate
	 */
	public Boolean getOncreate() {
		oncreate = (oncreate == null)? true: oncreate;
		return oncreate;
	}

	/**
	 * @param oncreate the oncreate to set
	 */
	public void setOncreate(Boolean oncreate) {
		this.oncreate = oncreate;
	}

	/**
	 * @return the onupdate
	 */
	public Boolean getOnupdate() {
		onupdate = (onupdate == null)? true: onupdate;
		return onupdate;
	}

	/**
	 * @param onupdate the onupdate to set
	 */
	public void setOnupdate(Boolean onupdate) {
		this.onupdate = onupdate;
	}

	public Boolean getMultiupload() {
		return multiupload;
	}

	public void setMultiupload(Boolean multiupload) {
		this.multiupload = new Boolean(multiupload);
	}

	public String getStyleName() {
		return styleName;
	}

	public void setStyleName(String styleName) {
		this.styleName = styleName;
	}	
	
	
	public String getLayout() {
		return layout;
	}
	
	public void setLayout(String layout) {
		this.layout = layout;
	}
	
	public void setOnmultiupload(Boolean onmultiupload) {
		this.onmultiupload = onmultiupload;
	}
		
	public Boolean getOnmultiupload() {
		return onmultiupload;
	}
		
	public void setOnsingleupload(Boolean onsingleupload) {
		this.onsingleupload = onsingleupload;
	}
	
	public Boolean getOnsingleupload() {
		return onsingleupload;
	}
	
}
