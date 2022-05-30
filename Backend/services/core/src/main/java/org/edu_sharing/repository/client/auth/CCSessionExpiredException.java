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
package org.edu_sharing.repository.client.auth;

import org.edu_sharing.repository.client.exception.CCException;


public class CCSessionExpiredException extends CCException{
	
	public CCSessionExpiredException(){
		super(CCException.SESSIONEXPIREDEXCEPTION,CCException.SESSIONEXPIREDEXCEPTION);
	}
	
	public CCSessionExpiredException(String message){
		super(CCException.SESSIONEXPIREDEXCEPTION,message);
	}
	
	
}
