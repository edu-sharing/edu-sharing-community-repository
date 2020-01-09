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
package org.edu_sharing.repository.server.jobs.quartz;

import java.io.File;
import java.io.OutputStream;
import java.io.StringWriter;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.metadataset.v2.MetadataReaderV2;
import org.edu_sharing.metadataset.v2.MetadataSetV2;
import org.edu_sharing.metadataset.v2.tools.MetadataHelper;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.StringTool;
import org.edu_sharing.repository.server.AuthenticationTool;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.MCAlfrescoBaseClient;
import org.edu_sharing.repository.server.RepoFactory;
import org.edu_sharing.repository.server.importer.OAIPMHLOMImporter;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.VCardConverter;
import org.edu_sharing.service.admin.AdminService;
import org.edu_sharing.service.admin.AdminServiceFactory;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class RefreshPublisherListJob extends AbstractJob {

	public static final String CONFIG_IGNORE_ENTRIES_KEY = "ignore_entries";
	public static final String CONFIG_VCARD_PROPS = "vcardprops";
	public static final String CONFIG_VALUESPACE_PROP = "valuespaceprop";
	public static final String CONFIG_FILEPATH = "filepath";
	
	Logger logger = Logger.getLogger(RefreshPublisherListJob.class);

	@Override
	public void execute(JobExecutionContext jobContext) throws JobExecutionException {
		JobDataMap jobDataMap = jobContext.getJobDetail().getJobDataMap();

		String ignoreEntries = (String)jobDataMap.get(CONFIG_IGNORE_ENTRIES_KEY);
		String vcardProps = (String)jobDataMap.get(CONFIG_VCARD_PROPS);
		String valuespaceProp = (String)jobDataMap.get(CONFIG_VALUESPACE_PROP);
		
		//String filePath = "tomcat/shared/classes/org/edu_sharing/metadataset/valuespace_learnline2_0_contributer.xml";
		String filePath = (String)jobDataMap.get(CONFIG_FILEPATH);
		
		if(vcardProps == null || vcardProps.trim().equals("")){
			throw new JobExecutionException(CONFIG_VCARD_PROPS + " is a mandatory!");
		}
		
		if(valuespaceProp == null || valuespaceProp.trim().equals("")){
			throw new JobExecutionException(CONFIG_VALUESPACE_PROP + " is a mandatory!");
		}
		
		if(filePath == null || filePath.trim().equals("")){
			throw new JobExecutionException(CONFIG_FILEPATH + " is a mandatory!");
		}
		
		
		
		ApplicationInfo homeRep  = ApplicationInfoList.getHomeRepository();
		try{
			AuthenticationTool authTool = RepoFactory.getAuthenticationToolInstance(homeRep.getAppId());
			HashMap<String, String>  authInfo = authTool.createNewSession(homeRep.getUsername(), homeRep.getPassword());
			
			AdminServiceFactory.getInstance().writePublisherToMDSXml(vcardProps, valuespaceProp, ignoreEntries, filePath, authInfo);

			MetadataReaderV2.refresh();
		}catch(org.alfresco.webservice.authentication.AuthenticationFault e){
			logger.error("AuthenticationFault while excecuting RefreshPublisherListJob:"+e.getMessage());
			throw new JobExecutionException(e);
		}catch(Throwable e){
			logger.error("Throwable while excecuting RefreshPublisherListJob"+e.getMessage());
			throw new JobExecutionException(e);
		}
	}

	@Override
	public Class[] getJobClasses() {
		// TODO Auto-generated method stub
		return allJobs;
	}

	
}
