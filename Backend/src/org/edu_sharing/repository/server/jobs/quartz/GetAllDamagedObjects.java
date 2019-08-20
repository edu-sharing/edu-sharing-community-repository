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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.commons.logging.LogFactory;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.importer.ImportCleaner;
import org.edu_sharing.repository.server.importer.PersistentHandlerEdusharing;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;


public class GetAllDamagedObjects extends AbstractJob {

	public GetAllDamagedObjects() {
		this.logger = LogFactory.getLog(GetAllDamagedObjects.class);
	}
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		logger.info("starting");
		Map jobDataMap = context.getJobDetail().getJobDataMap();
		
		String oaiBaseUrl = (String)jobDataMap.get(OAIConst.PARAM_OAI_BASE_URL);
		if(oaiBaseUrl == null || oaiBaseUrl.trim().equals("")){
			logger.warn("no oaiBaseUrl configured. will do nothing. end");
			return;
		}
		
		Object catalogsParamObj = jobDataMap.get(OAIConst.PARAM_OAI_CATALOG_IDS);
		if(catalogsParamObj == null){
			logger.warn("no allowed catalogs configured. will do nothing. end");
			return;
		}
		
		String metadataPrefix = (String)jobDataMap.get(OAIConst.PARAM_OAI_METADATA_PREFIX);
		
		List<NodeRef> allNodes = null;
		try{
			allNodes = new PersistentHandlerEdusharing(this,null,true).getAllNodesInImportfolder();
		}catch(Throwable e){
			logger.error("error while getting all nodes in Import Folder",e);
		}
		
		List<String> catalogsList = null;
		if(catalogsParamObj instanceof String){
			catalogsList = new ArrayList(Arrays.asList(((String)catalogsParamObj).split(",")));
		}else{
			catalogsList = (List)catalogsParamObj;
		}
		
		if(allNodes != null){
			for(NodeRef entry : allNodes){
				String alfNodeId = entry.getId();
				String importedKatalog = NodeServiceHelper.getProperty(entry,CCConstants.CCM_PROP_IO_REPLICATIONSOURCE);
				String importedId = NodeServiceHelper.getProperty(entry,CCConstants.CCM_PROP_IO_REPLICATIONSOURCEID);

				if(importedKatalog != null && importedKatalog.equals("DE.FWU")){
					try{
						ImportCleaner implCleaner = new ImportCleaner(oaiBaseUrl, catalogsList,metadataPrefix);
						boolean exists = implCleaner.nodeExists(importedId, importedKatalog);
						if(!exists){
							logger.info("mportedId:"+importedId+ " importedKatalog:"+importedKatalog +" alfNodeId:"+alfNodeId+" DOES NOT EXIST");
						}
					}catch(Throwable e){
						logger.error("error with importedId:"+importedId+ " importedKatalog:"+importedKatalog+" alf nodeId:"+alfNodeId,e);
					}
				}
				
			}
		}else{
			logger.error("allNodes is null");
		}
		
		logger.info("returning");
		
	}
	
	@Override
	public Class[] getJobClasses() {
		// TODO Auto-generated method stub
		return super.allJobs;
	}
}
