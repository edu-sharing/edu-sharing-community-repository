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
package org.edu_sharing.repository.server.tools.search;

import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetQueryProperty;
import org.edu_sharing.repository.client.rpc.metadataset.Validator;


public class QueryValidationFailedException extends Exception {
	
	Validator validator = null;
	
	String value;
	
	MetadataSetQueryProperty mdsqp = null;
	
	public QueryValidationFailedException(){
		super("validation failed");
	}
	
	public QueryValidationFailedException(Validator validator, String value, MetadataSetQueryProperty mdsqp) {
		super("validation failed");
		this.mdsqp = mdsqp;
		this.value = value;
		this.validator = validator;
	}

	public Validator getValidator() {
		return validator;
	}

	public void setValidator(Validator validator) {
		this.validator = validator;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public MetadataSetQueryProperty getMdsqp() {
		return mdsqp;
	}

	public void setMdsqp(MetadataSetQueryProperty mdsqp) {
		this.mdsqp = mdsqp;
	}
	
	
	
}
