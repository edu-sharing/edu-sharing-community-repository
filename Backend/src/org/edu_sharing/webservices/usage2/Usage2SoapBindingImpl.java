/**
 * Usage2SoapBindingImpl.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.usage2;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.rpc.ServiceException;

import org.apache.axis.client.Stub;
import org.apache.axis.message.SOAPHeaderElement;
import org.apache.commons.codec.binary.Base64;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.security.Signing;
import org.edu_sharing.service.usage.UsageException;

public class Usage2SoapBindingImpl implements org.edu_sharing.webservices.usage2.Usage2{
	
	private void validate(String eduRef) throws Usage2Exception{
		if(eduRef == null){
			Usage2Exception u2 = new Usage2Exception(new Exception("invalid eduRef null"));
			u2.setFaultReason("invalid eduRef null");
			throw u2;
		}
		
		if(!eduRef.matches("ccrep://[a-zA-Z0-9._-]*/[a-zA-Z0-9-]*")){
			Usage2Exception u2 = new Usage2Exception(new Exception("invalid eduRef"));
			u2.setFaultReason("invalid eduRef");
			throw u2;
		}
	}
	
	private String getNodeId(String eduRef) throws Usage2Exception{
		
		validate(eduRef);
		
		String[] splitted = eduRef.split("/");
		
		return splitted[splitted.length - 1]; 
	}
	
	private String getRepositoryId(String eduRef) throws Usage2Exception{
		String repoId = eduRef.replaceFirst("ccrep://", "");
		
		String nodeId = getNodeId(eduRef);
		repoId = repoId.replaceAll("/"+nodeId,"");
		return repoId;
	}
	
	public static void main(String[] args){
		String ccrep = "ccrep://localhost/ed730ce3-6821-4e02-b980-8c945b42c612";
		try{
			System.out.println(new Usage2SoapBindingImpl().getNodeId(ccrep));
			System.out.println(new Usage2SoapBindingImpl().getRepositoryId(ccrep));
		
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	
	
    public Usage2Result getUsage(String eduRef, String lmsId, String courseId, String user, String resourceId) throws java.rmi.RemoteException, Usage2Exception {
        
    	String repoId = getRepositoryId(eduRef);
    	
    	String parentNodeId = getNodeId(eduRef);
    	
    	//local
    	if(ApplicationInfoList.getHomeRepository().getAppId().equals(repoId)){
    		try{
    			return transform(new org.edu_sharing.service.usage.Usage2Service().getUsage(lmsId, courseId, parentNodeId, resourceId));
    		}catch(Usage2Exception e){
    			throw e;
    		}
    	//remote
    	}else{
    		ApplicationInfo appInfo = ApplicationInfoList.getRepositoryInfoById(repoId);
    		if(appInfo != null){
    			Usage2ServiceLocator locator = new Usage2ServiceLocator();
    			locator.setusage2EndpointAddress(appInfo.getWebServiceHotUrl()+"usage2");
    			try{
    				Usage2 u2 = locator.getusage2();
    				
    				sign((Stub)u2);
    				
    				return u2.getUsage(eduRef, lmsId, courseId, user, resourceId);
    			}catch(Exception e){
    				Usage2Exception e2 = new Usage2Exception(e);
        			e2.setFaultString(e.getMessage());
        			throw e2;
    			}
    	//unknown
    		}else{
    			Usage2Exception e2 = new Usage2Exception(new Exception("unknown application " + repoId));
    			e2.setFaultString("unknown application " + repoId);
    			throw e2;
    		}
    	}
    	
  	
    }

    
    
    public Usage2Result[] getUsagesByEduRef(String eduRef, String user) throws java.rmi.RemoteException, Usage2Exception {
    	String repoId = getRepositoryId(eduRef);
    	String parentNodeId = getNodeId(eduRef);
    	if(ApplicationInfoList.getHomeRepository().getAppId().equals(repoId)){
    		try{
    			
    			ArrayList<Usage2Result> result = new ArrayList<Usage2Result>();
    			List<org.edu_sharing.service.usage.Usage> usages = new org.edu_sharing.service.usage.Usage2Service().getUsageByParentNodeId(repoId, user, parentNodeId);
    			
    			for(org.edu_sharing.service.usage.Usage u : usages){
    				result.add(transform(u));
    			}
    			
    			
    			return result.toArray(new Usage2Result[result.size()]);
    		}catch(UsageException e){
    			Usage2Exception e2 = new Usage2Exception(e);
    			e2.setFaultString(e.getMessage());
    			throw e2;
    		}
    	//remote
    	}else{
    		ApplicationInfo appInfo = ApplicationInfoList.getRepositoryInfoById(repoId);
    		if(appInfo != null){
    			Usage2ServiceLocator locator = new Usage2ServiceLocator();
    			locator.setusage2EndpointAddress(appInfo.getWebServiceHotUrl()+"usage2");
    			try{
    				Usage2 u2 = locator.getusage2();
    				sign((Stub)u2);
    				return u2.getUsagesByEduRef(eduRef,user);
    			}catch(Exception e){
    				Usage2Exception e2 = new Usage2Exception(e);
        			e2.setFaultString(e.getMessage());
        			throw e2;
    			}
    	//unknown
    		}else{
    			Usage2Exception e2 = new Usage2Exception(new Exception("unknown application " + repoId));
    			e2.setFaultString("unknown application " + repoId);
    			throw e2;
    		}
    	}
    }

    public boolean deleteUsage(String eduRef, String user, String lmsId, String courseId, String resourceId) throws java.rmi.RemoteException, Usage2Exception {
    	
    	String repoId = getRepositoryId(eduRef);
    	String parentNodeId = getNodeId(eduRef);
    	
    	if(ApplicationInfoList.getHomeRepository().getAppId().equals(repoId)){
    		try{
    			
    			return new org.edu_sharing.service.usage.Usage2Service().deleteUsage(repoId, user, lmsId, courseId, parentNodeId, resourceId);
    			
    		}catch(UsageException e){
    			Usage2Exception e2 = new Usage2Exception(e);
    			e2.setFaultString(e.getMessage());
    			throw e2;
    		}
    	//remote
    	}else{
    		ApplicationInfo appInfo = ApplicationInfoList.getRepositoryInfoById(repoId);
    		if(appInfo != null){
    			Usage2ServiceLocator locator = new Usage2ServiceLocator();
    			locator.setusage2EndpointAddress(appInfo.getWebServiceHotUrl()+"usage2");
    			try{
    				Usage2 u2 = locator.getusage2();
    				sign((Stub)u2);
    				return u2.deleteUsage(eduRef, user, lmsId, courseId, resourceId);
    			}catch(Exception e){
    				Usage2Exception e2 = new Usage2Exception(e);
        			e2.setFaultString(e.getMessage());
        			throw e2;
    			}
    	//unknown
    		}else{
    			Usage2Exception e2 = new Usage2Exception(new Exception("unknown application " + repoId));
    			e2.setFaultString("unknown application " + repoId);
    			throw e2;
    		}
    	}
    }

    public Usage2Result setUsage(String eduRef, String user, String lmsId, String courseId, String userMail, java.util.Calendar fromUsed, java.util.Calendar toUsed, int distinctPersons, String version, String resourceId, String xmlParams) throws java.rmi.RemoteException, Usage2Exception {
    	
    	String repoId = getRepositoryId(eduRef);
    	String parentNodeId = getNodeId(eduRef);
    	
    	if(ApplicationInfoList.getHomeRepository().getAppId().equals(repoId)){
    		try{
    			
    			org.edu_sharing.service.usage.Usage localUsage = new org.edu_sharing.service.usage.Usage2Service().setUsage(repoId, user, lmsId, courseId, parentNodeId, userMail, fromUsed, toUsed, distinctPersons, version, resourceId, xmlParams);
    			return transform(localUsage);
    		}catch(UsageException e){
    			Usage2Exception e2 = new Usage2Exception(e);
    			e2.setFaultString(e.getMessage());
    			throw e2;
    		}
    	//remote
    	}else{
    		ApplicationInfo appInfo = ApplicationInfoList.getRepositoryInfoById(repoId);
    		if(appInfo != null){
    			Usage2ServiceLocator locator = new Usage2ServiceLocator();
    			locator.setusage2EndpointAddress(appInfo.getWebServiceHotUrl()+"usage2");
    			try{
    				Usage2 u2 = locator.getusage2();
    				sign((Stub)u2);
    				return u2.setUsage(eduRef, user, lmsId, courseId, userMail, fromUsed, toUsed, distinctPersons, version, resourceId, xmlParams);
    			}catch(Exception e){
    				Usage2Exception e2 = new Usage2Exception(e);
        			e2.setFaultString(e.getMessage());
        			throw e2;
    			}
    	//unknown
    		}else{
    			Usage2Exception e2 = new Usage2Exception(new Exception("unknown application " + repoId));
    			e2.setFaultString("unknown application " + repoId);
    			throw e2;
    		}
    	}
    	
    }
    
    private void sign(Stub stub) throws GeneralSecurityException{
    	
    	String timestamp = ""+System.currentTimeMillis();
		
		//sign essuid so that man in the middle can not change webservice data, essuid must be tested on serverside
		String signData = ApplicationInfoList.getHomeRepository().getAppId()+timestamp;
		
		Signing signing = new Signing();
		
		byte[] signature = signing.sign(signing.getPemPrivateKey(ApplicationInfoList.getHomeRepository().getPrivateKey(), CCConstants.SECURITY_KEY_ALGORITHM), signData, CCConstants.SECURITY_SIGN_ALGORITHM);
		signature = new Base64().encode(signature);
		
		((Stub)stub).setHeader(new SOAPHeaderElement("http://webservices.edu_sharing.org","timestamp",timestamp));
		((Stub)stub).setHeader(new SOAPHeaderElement("http://webservices.edu_sharing.org","appId",ApplicationInfoList.getHomeRepository().getAppId()));
		((Stub)stub).setHeader(new SOAPHeaderElement("http://webservices.edu_sharing.org","signature",new String(signature)));
		((Stub)stub).setHeader(new SOAPHeaderElement("http://webservices.edu_sharing.org","signed",signData));
    	
    	
    }

    
    
    private Usage2Result transform(org.edu_sharing.service.usage.Usage serviceUsage) {
		Usage2Result result = new Usage2Result();

		result.setAppUser(serviceUsage.getAppUser());
		result.setAppUserMail(serviceUsage.getAppUserMail());
		result.setCourseId(serviceUsage.getCourseId());
		result.setDistinctPersons(serviceUsage.getDistinctPersons());
		result.setFromUsed(serviceUsage.getFromUsed());
		result.setLmsId(serviceUsage.getLmsId());
		result.setNodeId(serviceUsage.getNodeId());
		result.setParentNodeId(serviceUsage.getParentNodeId());
		result.setResourceId(serviceUsage.getResourceId());
		result.setToUsed(serviceUsage.getToUsed());
		result.setUsageCounter(serviceUsage.getUsageCounter());
		result.setUsageVersion(serviceUsage.getUsageVersion());
		result.setUsageXmlParams(serviceUsage.getUsageXmlParams());
		return result;
	}
}
