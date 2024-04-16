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
package org.edu_sharing.repository.server.tools;

import org.apache.commons.codec.digest.DigestUtils;

public class KeyTool {
	public String getKey(){
		Long currentMillis = System.currentTimeMillis();
		Long randomLong = (long)(Math.random() * 1000000.0);
		
		String data = currentMillis.toString() + randomLong.toString();
		return DigestUtils.md5Hex(data);
	}
	
	public String getRandomPassword(){
		
		return getKey();
	}
}
