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

/**
 * @author rudolph
 */
public class MetadataSetModelProperty implements com.google.gwt.user.client.rpc.IsSerializable {
	
	String name = null;
	String datatype = null;
	String processtype = null;
	String copyprop = null;
	String defaultValue = null;
	Boolean concatewithtype = null;
	Boolean oncreate = null;
	Boolean onupdate = null;
	String keyContenturl;
	Boolean multiple = null;
	Boolean multilang = null;
	
	//from or to
	String assocType = null;
	
	public static final String PROCESSTYPE_STANDARD = "standard";
	public static final String PROCESSTYPE_COPYFROMPARENT = "copyfromparent";
	public static final String PROCESSTYPE_COPYFROMREQUEST = "copyfromrequest";
	
	//for props that are not in request but are available when the node is created. for example createdate
	public static final String PROCESSTYPE_COPYFROMYOURSELF = "copyfromyourself";
	
	public static final String PROCESSTYPE_DEFAULTVALUE = "defaultvalue";
	public static final String PROCESSTYPE_UPLOAD = "upload";
	public static final String PROCESSTYPE_CHILDASSOC = "childassoc";
	public static final String PROCESSTYPE_ASSOC = "assoc";
	public static final String PROCESSTYPE_ASPECT ="CC_FORM_ASPECT";
	public static final String PROCESSTYPE_HELPER ="helper";
	
	public static final String DATATYPE_STRING = "string";
	public static final String DATATYPE_DATE = "date";
	public static final String DATATYPE_LONG = "int";
	public static final String DATATYPE_BOOLEAN = "boolean";
	public static final String DATATYPE_BINARY = "binary";
	
	MetadataSetModelType parent;

	public MetadataSetModelProperty() {
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
	 * @return the datatype
	 */
	public String getDatatype() {
		return datatype;
	}
	/**
	 * @param datatype the datatype to set
	 */
	public void setDatatype(String datatype) {
		this.datatype = datatype;
	}
	/**
	 * @return the processtype
	 */
	public String getProcesstype() {
		return processtype;
	}
	/**
	 * @param processtype the processtype to set
	 */
	public void setProcesstype(String processtype) {
		this.processtype = processtype;
	}
	/**
	 * @return the copyprop
	 */
	public String getCopyprop() {
		return copyprop;
	}
	/**
	 * @param copyprop the copyprop to set
	 */
	public void setCopyprop(String copyprop) {
		this.copyprop = copyprop;
	}
	/**
	 * @return the concatewithtype
	 */
	public Boolean getConcatewithtype() {
		if(concatewithtype == null) return false;
		return concatewithtype;
	}
	/**
	 * @param concatewithtype the concatewithtype to set
	 */
	public void setConcatewithtype(Boolean concatewithtype) {
		this.concatewithtype = concatewithtype;
	}

	/**
	 * @return the defaultValue
	 */
	public String getDefaultValue() {
		return defaultValue;
	}

	/**
	 * @param defaultValue the defaultValue to set
	 */
	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	/**
	 * @return the parent
	 */
	public MetadataSetModelType getParent() {
		return parent;
	}

	/**
	 * @param parent the parent to set
	 */
	public void setParent(MetadataSetModelType parent) {
		this.parent = parent;
	}

	/**
	 * @return the oncreate
	 */
	public Boolean getOncreate() {
		
		if(oncreate == null) return true;
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
		if(onupdate == null) return true;
		return onupdate;
	}

	/**
	 * @param onupdate the onupdate to set
	 */
	public void setOnupdate(Boolean onupdate) {
		this.onupdate = onupdate;
	}

	/**
	 * @return the keyContenturl
	 */
	public String getKeyContenturl() {
		return keyContenturl;
	}

	/**
	 * @param keyContenturl the keyContenturl to set
	 */
	public void setKeyContenturl(String keyContenturl) {
		this.keyContenturl = keyContenturl;
	}

	/**
	 * @return the multiple
	 */
	public Boolean getMultiple() {
		if(multiple == null) return false;
		return multiple;
	}

	/**
	 * @param multiple the multiple to set
	 */
	public void setMultiple(Boolean multiple) {
		this.multiple = multiple;
	}
	
	public String getFormElementName(){
		
		String type = getParent().getType(); 
		String formElementName = (getConcatewithtype()) ? type+"#"+name:name;
		return formElementName;
	}

	/**
	 * @return the assocType
	 */
	public String getAssocType() {
		return assocType;
	}

	/**
	 * @param assocType the assocType to set
	 */
	public void setAssocType(String assocType) {
		this.assocType = assocType;
	}

	/**
	 * @return the multilang
	 */
	public Boolean getMultilang() {
		if(multilang == null) return false;
		return multilang;
	}

	/**
	 * @param multilang the multilang to set
	 */
	public void setMultilang(Boolean multilang) {
		this.multilang = multilang;
	}
	
}
