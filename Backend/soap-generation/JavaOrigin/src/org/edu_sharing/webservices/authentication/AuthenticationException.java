/*
 * 
 */

package org.edu_sharing.webservices.authentication;

import org.apache.axis.AxisFault;

public class AuthenticationException extends AxisFault implements java.io.Serializable {
  
	String message;
	
	Throwable cause;
	
    public AuthenticationException(String message) {
    	this.message = message;
    	
    }
    
    public String getMessage() {
    	// TODO Auto-generated method stub
    	return this.message;
    }
    
    public Throwable getCause() {
		return cause;
	}

}
