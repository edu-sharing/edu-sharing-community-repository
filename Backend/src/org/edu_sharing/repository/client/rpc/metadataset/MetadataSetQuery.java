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

import java.util.List;

public class MetadataSetQuery  implements com.google.gwt.user.client.rpc.IsSerializable {
	
	public static final String DEFAULT_CRITERIABOXID = "topAreaView";//"search2_pw_search";
	
	List<MetadataSetQueryProperty> properties;
	String join;
	String statement;
	String layout;
	String handlerclass;
	String criteriaboxid;
	String stylename;
	String widget;
	
	
	
	MetadataSetValue label;
	
	MetadataSetQueries parent = null;
	
	
	
	public List<MetadataSetQueryProperty> getProperties() {
		return properties;
	}
	public void setProperties(List<MetadataSetQueryProperty> properties) {
		this.properties = properties;
	}
	public String getJoin() {
		return join;
	}
	public void setJoin(String join) {
		this.join = join;
	}
	public String getStatement() {
		return statement;
	}
	public void setStatement(String statement) {
		this.statement = statement;
	}
	public MetadataSetValue getLabel() {
		return label;
	}
	public void setLabel(MetadataSetValue label) {
		this.label = label;
	}
	public String getLayout() {
		return layout;
	}
	public void setLayout(String layout) {
		this.layout = layout;
	}
	public String getHandlerclass() {
		return handlerclass;
	}
	public void setHandlerclass(String handlerclass) {
		this.handlerclass = handlerclass;
	}
	public String getCriteriaboxid() {
		return criteriaboxid;
	}
	public void setCriteriaboxid(String criteriaboxid) {
		this.criteriaboxid = criteriaboxid;
	}
	public String getStylename() {
		return stylename;
	}
	public void setStylename(String stylename) {
		this.stylename = stylename;
	}
	public MetadataSetQueries getParent() {
		return parent;
	}
	public void setParent(MetadataSetQueries parent) {
		this.parent = parent;
	}
	public String getWidget() {
		return widget;
	}
	public void setWidget(String widget) {
		this.widget = widget;
	}
	
}
