package org.edu_sharing.repository.server.tools.forms;

import java.util.TimeZone;

import org.apache.commons.fileupload.FileItem;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetModelProperty;
import org.edu_sharing.repository.client.tools.DateParser;
import org.edu_sharing.repository.server.tools.StringTool;

public class ServletValueTool {

	Logger logger = Logger.getLogger(ServletValueTool.class);
	
	public String getValue(FileItem value, String dataType){
		String result = null;
		
		//unchecked checkbox
		if(dataType.equals(MetadataSetModelProperty.DATATYPE_BOOLEAN) && (value == null || value.getString() == null)){
			return "false";
		}
		
		if(value != null && value.getString() != null){	
			//logger.info("propsToSafe:"+property + " " + value.getString());
			/**
			 * when the html file got's
			 * 
			 * <meta content="text/html; charset=UTF-8" http-equiv="content-type" />
			 * 
			 * you must transfrom Strings to UTF-8
			 */
			
			if(dataType.equals(MetadataSetModelProperty.DATATYPE_BOOLEAN) && value.getString().equals("on")){
				result = "true";
			}else if(dataType.equals(MetadataSetModelProperty.DATATYPE_LONG)){
				//check if its a correct number
				try{
					new Long(value.getString());
					result = value.getString();
				}catch(NumberFormatException e){
					logger.info(value.getString() +" was not a number!");
					result = null;
				}
			}else if(dataType.equals(MetadataSetModelProperty.DATATYPE_DATE)){
				//add the UTC offset to ISO Date
				String isoDate = StringTool.getEncoded(value.get());
				
				if(!isoDate.trim().equals("")){
					//@todo validate date
					
					//calculate the offset depending on the servers timzone
					//offset:
					int hoursToAdd = ((TimeZone.getDefault().getRawOffset() / 1000) / 60) / 60;
					//summertime/wintertime
					int dstToAdd = ((TimeZone.getDefault().getDSTSavings() / 1000) / 60) /60;
					hoursToAdd+= dstToAdd;
					String hourString = (new Integer(hoursToAdd).toString().length() == 1) ? "+0"+hoursToAdd : "+"+new Integer(hoursToAdd).toString();
					
					if(isoDate.matches(DateParser.PATTERN_DATETIME_WITH_MS_AND_DZD)){
						result = isoDate;
					}else if(isoDate.matches(DateParser.PATTERN_DATETIME_WITH_MS)){
						isoDate+=hourString+":00";
						result = isoDate;
					}else if(isoDate.matches(DateParser.PATTERN_DATETIME)){
						isoDate+=".000"+hourString+":00";
						result = isoDate;
					}else{
						logger.error("invalid date format:"+isoDate);
						result = null;
					}
					logger.info(result);
				}else{
					result = null;
				}
				
			}else{
				result = StringTool.getEncoded(value.get());	
			}

			
		}else if(dataType.equals(MetadataSetModelProperty.DATATYPE_BOOLEAN)){
			result = "false";
		}
		return result;
	}
	
}
