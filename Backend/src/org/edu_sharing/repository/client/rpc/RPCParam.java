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
package org.edu_sharing.repository.client.rpc;

import java.util.HashMap;

public class RPCParam implements com.google.gwt.user.client.rpc.IsSerializable {
	
	String className = null;
	HashMap myObject = null;
	
	public RPCParam(){
		
	}
	
	public RPCParam(String _className,Object value){
		className = _className;
		myObject = new HashMap();
		myObject.put("1", value);
	}
	public String getClassName() {
		return className;
	}
	public void setClassName(String className) {
		this.className = className;
	}
	public Object getValue(){
		return myObject.get("1");
	}
	
	
	
}
