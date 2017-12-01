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

public class MetadataSetQueryProperty extends MetadataSetBaseProperty{
	
	String statement = null;
	
	String multiple = null;
	
	String multiplejoin = null;

	MetadataSetQuery parent = null;
	
	
	
	List<Validator> validators = null;
	
	
	
	
	String init_by_get_param = null;
	

	
	/**
	 * QueryBuilder escape lucene tokens. default is true
	 * 
	 */
	Boolean escape = true;
		
	public String getStatement() {
		return statement;
	}

	public void setStatement(String statement) {
		this.statement = statement;
	}

	public Boolean getMultiple() {
		if(multiple == null || !multiple.equals("true")) return false;
		else return true;
			
	}

	public void setMultiple(String multiple) {
		this.multiple = multiple;
	}

	public String getMultiplejoin() {
		return multiplejoin;
	}

	public void setMultiplejoin(String multiplejoin) {
		this.multiplejoin = multiplejoin;
	}

	public MetadataSetQuery getParent() {
		return parent;
	}

	public void setParent(MetadataSetQuery parent) {
		this.parent = parent;
	}

	
	
	/**
	 * QueryBuilder escape lucene tokens. default is true.
	 * Can be set when the statement string is in Quotes
	 * @return
	 */
	public void setEscape(String escape) {
		if(escape != null && !escape.trim().equals("")){
			this.escape = new Boolean(escape);
		}
	}

	/**
	 * QueryBuilder escape lucene tokens. default is true.
	 * @return
	 */
	public Boolean getEscape() {
		return escape;
	}


	public List<Validator> getValidators() {
		return validators;
	}

	public void setValidators(List<Validator> validators) {
		this.validators = validators;
	}

	public String getInit_by_get_param() {
		return init_by_get_param;
	}

	public void setInit_by_get_param(String init_by_get_param) {
		this.init_by_get_param = init_by_get_param;
	}
	
}
