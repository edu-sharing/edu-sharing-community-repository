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
 *
 */
public interface Validator extends com.google.gwt.user.client.rpc.IsSerializable {
	
	public static final String MANDATORY = "MANDATORY";
	
	public static final String MANDATORYTITLE = "MANDATORYTITLE";
	
	public static final String INT = "INT";
	
	public static final String DATE = "DATE";
	
	public static final String MINCHARS = "MINCHARS";
	
	public static final String MINIMAL_ONE_CRITERIA = "MINIMAL_ONE_CRITERIA"; 
	
	public boolean check(String value);
	public String getMessageId();
	
	
}
