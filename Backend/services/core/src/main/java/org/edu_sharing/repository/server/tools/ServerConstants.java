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

import java.text.DateFormat;
import java.util.Locale;

/**
 * @author rudolph
 *
 */
public class ServerConstants {
	
	public final static DateFormat DEFAULTDATEFORMAT = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT, Locale.GERMANY);
	
	
	public final static DateFormat DATEFORMAT_WITHOUT_TIME = DateFormat.getDateInstance(DateFormat.LONG, Locale.GERMANY);
}
