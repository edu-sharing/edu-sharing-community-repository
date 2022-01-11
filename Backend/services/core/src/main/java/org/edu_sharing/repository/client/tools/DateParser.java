package org.edu_sharing.repository.client.tools;

import java.util.Date;

public class DateParser {

	
	public static final String PATTERN_FUZZY_END = "[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]T[0-9][0-9]:[0-9][0-9]:[0-9][0-9][a-zA-Z0-9:+\\.]*";
	
	/**
	 * datetime pattern with milliseconds and Time zone designator
	 * 
	 * i.e. 2012-08-09T00:00:00.000+02:00
	 */
	public static final String PATTERN_DATETIME_WITH_MS_AND_DZD = "[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]T[0-9][0-9]:[0-9][0-9]:[0-9][0-9]\\.[0-9][0-9][0-9]\\+[0-9][0-9]:[0-9][0-9]";
	
	/**
	 * datetime pattern with milliseconds and NO Time zone designator
	 * 
	 * i.e. 2012-08-09T00:00:00.000
	 */
	public static final String PATTERN_DATETIME_WITH_MS = "[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]T[0-9][0-9]:[0-9][0-9]:[0-9][0-9]\\.[0-9][0-9][0-9]";
	
	/**
	 * datetime pattern without milliseconds and NO Time zone designator
	 * 
	 * i.e. 2012-08-09T00:00:00
	 */
	public static final String PATTERN_DATETIME = "[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]T[0-9][0-9]:[0-9][0-9]:[0-9][0-9]";
	/**
	 * parses an ISO Date
	 * i.e.:
	 * 
	 * 2012-06-15T00:00:00.0Z
	 * 
	 * 2012-06-15
	 * 
	 * @param isoDate
	 * @return
	 */
	public static Date getDate(String isoDate){
		
		
		//matches sth. like 2012-06-15T00:00:00.0Z
		if(isoDate.matches(PATTERN_FUZZY_END)){
			
			String yStr = isoDate.substring(0, 4);
			int year = new Integer(yStr);
			String mStr = isoDate.substring(5, 7);
			int month = new Integer(mStr);
			String dStr = isoDate.substring(8, 10);
			int day = new Integer(dStr);
			
			String hStr = isoDate.substring(11, 13);
			int hour = new Integer(hStr);
			String minStr = isoDate.substring(14, 16);
			int minute = new Integer(minStr);
			String secStr = isoDate.substring(17, 19);
			int sec = new Integer(secStr);
			
			//System.out.println("yStr:"+yStr+ " mStr:"+mStr+" dStr:"+dStr +" hStr:"+hStr+" minStr:"+minStr+" secStr:"+secStr);
			
			
			
			year = year - 1900;
			month = (month > 0) ? month - 1 : month;
			
			Date date = new Date(year,month,day,hour,minute,sec);
			
			
			
			return date;
		}
		
		//matches sth. like 2012-06-15
		//if(isoDate.matches("[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9][a-zA-Z0-9:\\.]*")){
		if(isoDate.matches("[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]")){
			
			String yStr = isoDate.substring(0, 4);
			int year = new Integer(yStr);
			String mStr = isoDate.substring(5, 7);
			int month = new Integer(mStr);
			String dStr = isoDate.substring(8, 10);
			int day = new Integer(dStr);
			
			year = year - 1900;
			month = (month > 0) ? month - 1 : month;
			
			Date date = new Date(year,month,day);
			
			return date;
		}	
		
		
		
		
		return null;
	}
	
	public static void main(String[] args){
		//DateParser.getDate("2012-06-15T00:00:00");
		DateParser.getDate("2012-06-15T10:15:30");
	}
}
