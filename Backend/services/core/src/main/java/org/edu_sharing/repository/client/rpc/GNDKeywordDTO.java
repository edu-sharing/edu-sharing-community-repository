package org.edu_sharing.repository.client.rpc;

import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.StringTool;

import java.io.Serializable;

public class GNDKeywordDTO implements Serializable, HasKey {

	String id;
	
	String value;
	
	String category;
	
	String categoryId;
	
	public GNDKeywordDTO(){
	}

	public String getCategory() {
		return category;
	}
	public String getId() {
		return id;
	}
	
	public String getValue() {
		return value;
	}
	
	public String getCategoryId() {
		return categoryId;
	}
	public void setCategory(String category) {
		this.category = category;
	}
	
	public void setCategoryId(String categoryId) {
		this.categoryId = categoryId;
	}
	public void setId(String id) {
		this.id = id;
	}
	public void setValue(String value) {
		this.value = value;
	}

	public String getDisplayString() {
		String cat = getCategory().replaceAll(StringTool.escape(CCConstants.MULTIVALUE_SEPARATOR), " ; ");
		String result = getValue() +"<span style=\"font-size:10px\"> ("+cat+")<span>";
		return result;
	}

	public String getReplacementString() {
		String cat = getCategory().replaceAll(StringTool.escape(CCConstants.MULTIVALUE_SEPARATOR), " ; ");
		return getValue() + " ("+ cat + ")";
	}
	
	@Override
	public String getKey() {
		return getId();
	}
}
