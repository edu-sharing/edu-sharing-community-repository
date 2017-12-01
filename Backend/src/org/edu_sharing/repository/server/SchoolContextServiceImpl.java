package org.edu_sharing.repository.server;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.SchoolContextService;
import org.edu_sharing.repository.client.rpc.SchoolContextValues;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.StringTool;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

public class SchoolContextServiceImpl extends RemoteServiceServlet implements SchoolContextService{

	Logger logger = Logger.getLogger(SchoolContextServiceImpl.class);
	
	/**
	 * for use in none servlet context
	 */
	MCAlfrescoBaseClient baseClient = null;
	
	public SchoolContextServiceImpl() {
	}
	
	public SchoolContextServiceImpl(MCAlfrescoBaseClient baseClient ) {
		this.baseClient = baseClient;
	}
	
	@Override
	public SchoolContextValues getSchoolContextValues(String nodeRefFederalState, String nodeRefTypeOfSchool, String nodeRefSchoolSubject, String nodeRefAgeGroup) {
		logger.info("start");
		
		try{
		
			org.edu_sharing.service.schoolcontext.SchoolContextService scs = new org.edu_sharing.service.schoolcontext.SchoolContextServiceImpl(getMCAlfrescoBaseClient(null));
			return scs.getSchoolConextValues(nodeRefFederalState, nodeRefTypeOfSchool, nodeRefSchoolSubject, nodeRefAgeGroup);
		
		}catch(Throwable e){
			logger.error(e.getMessage(),e);
		}
		
		return null;
	}
	
	public SchoolContextValues getSchoolContextValues() {
		
		try {
			
			org.edu_sharing.service.schoolcontext.SchoolContextService scs = new org.edu_sharing.service.schoolcontext.SchoolContextServiceImpl(getMCAlfrescoBaseClient(null));
			return scs.getSchoolConextValues();
		
		} catch(Throwable e) {
			logger.error(e.getMessage(),e);
		}
		
		return null;
	}
	
	/**
	 * @param nodeId
	 * @return
	 */
	public ArrayList<String[]> getSchoolContextDisplayPath(String nodeId){
		
		ArrayList<String[]> result = new ArrayList<String[]>();
		
		try{
			MCAlfrescoBaseClient alfClient = getMCAlfrescoBaseClient(null);
			String schoolContext = alfClient.getProperty("workspace", "SpacesStore", nodeId, CCConstants.CCM_PROP_IO_SCHOOLCONTEXT);
			if(schoolContext != null && !schoolContext.trim().equals("")){
				
				String[] splitted = schoolContext.split(StringTool.escape(CCConstants.MULTIVALUE_SEPARATOR));
				for(String path : splitted){
						String[] pathElements = path.split(CCConstants.SCHOOLCONTEXT_PATH_SEPARATOR);
						
						ArrayList<String> displayEles = new ArrayList<String>(); 
						for(String pathElement:pathElements){
							if(pathElement.startsWith("workspace://SpacesStore")){
								String entityNodeId = pathElement.replace("workspace://SpacesStore/", "");
								String title = alfClient.getProperty("workspace", "SpacesStore", entityNodeId, CCConstants.CM_PROP_TITLE);
								displayEles.add(title);
							}else{
								displayEles.add(pathElement);
							}
						}
						result.add(displayEles.toArray(new String[displayEles.size()]));
				}
				 
			}
		}catch(Throwable e){
			logger.error(e.getMessage(),e);
		}
		
		return result;
	}
	
	private MCAlfrescoBaseClient getMCAlfrescoBaseClient(String repositoryId) throws Throwable{
		
		if(baseClient == null){
			return (MCAlfrescoBaseClient)RepoFactory.getInstance(repositoryId,  this.perThreadRequest.get().getSession());
		}else{
			return baseClient;
		}
	}
	
}
