package org.edu_sharing.rest.usage;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Map;

import org.edu_sharing.repository.server.tools.DateTool;
import org.edu_sharing.service.usage.AuthenticationException;
import org.edu_sharing.service.usage.Usage;
import org.edu_sharing.service.usage.UsageException;
import org.edu_sharing.service.usage.UsageService;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Put;
import org.restlet.resource.ServerResource;

public class UsageResource extends ServerResource{
	
	public static final String PARAM_TICKET = "ticket";
	public static final String PARAM_APPUSER = "appUser";
	public static final String PARAM_APPUSER_MAIL = "appUserMail";
	public static final String PARAM_APPSESSION_ID = "appSessionId";
	public static final String PARAM_FROM_USED = "fromUsed";
	public static final String PARAM_TO_USED = "toUsed";
	public static final String PARAM_DISTINCT_PERSONS = "distinctPersons";
	public static final String PARAM_VERSION = "version";
	
	public static final String PARAM_XML = "xmlParams";
	
	
	
	public static final String ATT_IO_NODE_ID = "ioNodeId";
	public static final String ATT_LMS_ID = "lmsId";
	public static final String ATT_COURSE_ID = "courseId";
	public static final String ATT_RESOURCE_ID = "resourceId";
	
	
	
	public static final String RESTPATH_USAGE = "/usages/{"+UsageResource.ATT_IO_NODE_ID+"}/{"+UsageResource.ATT_LMS_ID+"}/{"+UsageResource.ATT_COURSE_ID+"}";
	
	public static final String RESTPATH_USAGE_RESOURCE = "/usages/{"+UsageResource.ATT_IO_NODE_ID+"}/{"+UsageResource.ATT_LMS_ID+"}/{"+UsageResource.ATT_COURSE_ID+"}/{"+ UsageResource.ATT_RESOURCE_ID+"}";
	
	public static final String RESTPATH_USAGES_IO = "/usages/{ioNodeId}";
	
	
	public static final String howToString = "address one usage entity for getUsage,setUsage,deleteUsage: <br/>"+RESTPATH_USAGE
	+ "<br><br> address one usage entity with lms resourceId for getUsage,setUsage,deleteUsage:<br/>"+RESTPATH_USAGE_RESOURCE
	+"<br><br> get all usages of an io:<br/>"+RESTPATH_USAGES_IO
	+"<br><br> use the following request params: " + PARAM_APPSESSION_ID +", "+ PARAM_APPUSER +", "+PARAM_APPUSER_MAIL+", "+PARAM_DISTINCT_PERSONS+", "+PARAM_FROM_USED+", "+PARAM_TICKET+", "+PARAM_TO_USED+", "+PARAM_VERSION+", "+PARAM_XML;
	
	@Get
	public String getUsages(){
		
		String ticket = this.getQuery().getValues(PARAM_TICKET);
		String appUser = this.getQuery().getValues(PARAM_APPUSER);
		
		Map<String,Object> reqAtts = this.getRequestAttributes();
		String ioNodeId = (String)reqAtts.get(ATT_IO_NODE_ID);
		String lmsId = (String)reqAtts.get(ATT_LMS_ID);
		String courseId = (String)reqAtts.get(ATT_COURSE_ID);
		//can be null:
		String resourceId = (String)reqAtts.get(ATT_RESOURCE_ID);
		String ip = this.getRequest().getClientInfo().getAddress();
		
		UsageService usageService = new UsageService(ip,lmsId,ticket);
		JSONObject jsonObj = new JSONObject();
		try{
			
			if(ioNodeId != null){
				try{
					if(lmsId == null){
						
						ArrayList<Usage> usages = usageService.getUsageByParentNodeId(ioNodeId);
						
						ArrayList<Map<String,String>> usagesAsMaps = new ArrayList<Map<String,String>>();
						for(Usage usage : usages){
							usagesAsMaps.add(usage.toMap());
						}
						jsonObj.put("usages", usagesAsMaps);
						return jsonObj.toString();
						
					}else if(courseId != null){
						
						Usage usage = usageService.getUsage(courseId, ioNodeId, appUser, resourceId);
						jsonObj.put("usage", usage.toMap());
						return jsonObj.toString();
					}else{
						
						jsonObj.put("error", "MISSING_COURSE_ID");
						return jsonObj.toString();
					}
				}catch(AuthenticationException e){
					e.printStackTrace();
					
					jsonObj.put("error", e.getMessage());
					return jsonObj.toString();
				}catch(UsageException e){
					e.printStackTrace();
					
					jsonObj.put("error", e.getMessage());
					return jsonObj.toString();
				}catch(Throwable e){
					e.printStackTrace();
					jsonObj.put("error", e.getMessage());
					return jsonObj.toString();
				}
				
			}else{
				
				
				
				jsonObj.put("error", "MISSING_IO_NODEID");
				
				return jsonObj.toString();
			}
		}catch(JSONException e){
			e.printStackTrace();
			try{
				jsonObj.put("error", e.getMessage());
			}catch(JSONException e2){
				e2.printStackTrace();
			}
			return jsonObj.toString();
		}
	}
	
	@Put
	public String putUsage(){
		String ticket = this.getQuery().getValues(PARAM_TICKET);
		String appUser = this.getQuery().getValues(PARAM_APPUSER);
		String appUserMail = this.getQuery().getValues(PARAM_APPUSER_MAIL);
		String fromUsed =  this.getQuery().getValues(PARAM_FROM_USED);
		String toUsed =  this.getQuery().getValues(PARAM_TO_USED);
		String distinctPersons = this.getQuery().getValues(PARAM_DISTINCT_PERSONS);
		String version = this.getQuery().getValues(PARAM_VERSION);
		String xmlParams = this.getQuery().getValues(PARAM_XML);
		
		
		Map<String,Object> reqAtts = this.getRequestAttributes();
		String ioNodeId = (String)reqAtts.get(ATT_IO_NODE_ID);
		String lmsId = (String)reqAtts.get(ATT_LMS_ID);
		String courseId = (String)reqAtts.get(ATT_COURSE_ID);
		String resourceId = (String)reqAtts.get(ATT_RESOURCE_ID);
		
		String ip = this.getRequest().getClientInfo().getAddress();
		
		UsageService usageService = new UsageService(ip,lmsId,ticket);
		
		Calendar fromUsedCalendar = null;
		if(fromUsed != null){
			fromUsedCalendar =  Calendar.getInstance();
			fromUsedCalendar.setTime(new DateTool().getDate(fromUsed));
		}
		Calendar toUsedCalendar = null;
		if(toUsed != null){
			toUsedCalendar =  Calendar.getInstance();
			toUsedCalendar.setTime(new DateTool().getDate(toUsed));
		}
		
		Integer distinctPersonsInt = null;
		if(distinctPersons != null){
			distinctPersonsInt = new Integer(distinctPersons);
		}
		
		JSONObject result = new JSONObject();
		try{
			try{
			
				usageService.setUsage(courseId, ioNodeId, appUser, appUserMail, fromUsedCalendar, toUsedCalendar, distinctPersonsInt, version, resourceId, xmlParams);
				result.put("success","new usage created");
				return result.toString();
			}catch(AuthenticationException e){
				e.printStackTrace();
				result.put("error",e.getMessage());
			}catch(UsageException e){
				e.printStackTrace();
				result.put("error",e.getMessage());
			}catch(Throwable e){
				e.printStackTrace();
				result.put("error",e.getMessage());
			}
			
		}catch(JSONException e){
			try{
				result.put("error", e.getMessage());
			}catch(JSONException e2){
				e2.printStackTrace();
			}
		}
		return result.toString();
	}
	
	@Delete
	public String removeUsage(){
		String ticket = this.getQuery().getValues(PARAM_TICKET);
		String appUser = this.getQuery().getValues(PARAM_APPUSER);
		String appSessionId =  this.getQuery().getValues(PARAM_APPSESSION_ID);
		 
		
		Map<String,Object> reqAtts = this.getRequestAttributes();
		String ioNodeId = (String)reqAtts.get(ATT_IO_NODE_ID);
		String lmsId = (String)reqAtts.get(ATT_LMS_ID);
		String courseId = (String)reqAtts.get(ATT_COURSE_ID);
		String resourceId = (String)reqAtts.get(ATT_RESOURCE_ID);
	
		String ip = this.getRequest().getClientInfo().getAddress();
		
		UsageService usageService = new UsageService(ip,lmsId,ticket);
		JSONObject result = new JSONObject();
		try{
			try{
				boolean resultdelete = usageService.deleteUsage(appSessionId, appUser, courseId, ioNodeId, resourceId);				
				result.put("result", resultdelete);
			}catch(AuthenticationException e){
				e.printStackTrace();
				result.put("error",e.getMessage());
			}catch(UsageException e){
				e.printStackTrace();
				result.put("error",e.getMessage());
			}catch(Throwable e){
				e.printStackTrace();
				result.put("error",e.getMessage());
			}
		}catch(JSONException e){
			try{
				result.put("error", e.getMessage());
			}catch(JSONException e2){
				e2.printStackTrace();
			}
		}
		return result.toString();
	}
}
