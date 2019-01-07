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

import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.server.importer.BinaryHandler;
import org.edu_sharing.repository.server.importer.Importer;
import org.edu_sharing.repository.server.importer.OAIPMHLOMImporter;
import org.edu_sharing.repository.server.importer.PersistentHandlerEdusharing;
import org.edu_sharing.repository.server.importer.RecordHandlerInterface;
import org.edu_sharing.repository.server.importer.RecordHandlerLOM;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class ImporterJob extends AbstractJob {

	public static Logger logger=Logger.getLogger(ImporterJob.class);
	private JobExecutionContext context;

	public ImporterJob() {

	}
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		super.execute(context);
		String username = (String) context.getJobDetail().getJobDataMap().get(OAIConst.PARAM_USERNAME);
		
		if(username == null || username.trim().equals("")) {
			throw new JobExecutionException("no user provided");
		}

		RunAsWork<Void> runAs = new RunAsWork<Void>() {

			@Override
			public Void doWork() throws Exception {
				start(context,context.getJobDetail().getJobDataMap());
				return null;
			}

		};
		AuthenticationUtil.runAs(runAs, username);
		
	}
	public String start(JobDataMap jobDataMap){
		this.jobDataMap=jobDataMap;
		return start(null,jobDataMap);
	}
	private String start(JobExecutionContext context, Map jobDataMap) {
		logger.info("starting");

		Object setsParamObj = jobDataMap.get(OAIConst.PARAM_OAI_SETS);
		// String setsParam = (String) jobDataMap.get("sets");
		List<String> splitted = null;
		if (setsParamObj instanceof String) {
			splitted = new ArrayList(Arrays.asList(((String) setsParamObj).split(",")));
		} else {
			splitted = (List) setsParamObj;
		}

		String urlImport = null;
		String[] sets = null;
		if(splitted!=null) {
			for (String set : splitted) {
				if (set.contains("http://")) {
					urlImport = set.trim();
				}
			}
			if (urlImport != null) {
				splitted.remove(urlImport);
			}
			sets = splitted.toArray(new String[splitted.size()]);
		}
		String oaiBaseUrl = (String) jobDataMap.get(OAIConst.PARAM_OAI_BASE_URL);
		String metadataPrefix = (String) jobDataMap.get(OAIConst.PARAM_OAI_METADATA_PREFIX);
		metadataPrefix = (metadataPrefix == null || metadataPrefix.trim().equals("")) ? "oai_lom-de" : metadataPrefix;
		String metadataSetId = (String) jobDataMap.get(OAIConst.PARAM_METADATASET_ID);

		String recordHandlerClass = (String) jobDataMap.get(OAIConst.PARAM_RECORDHANDLER);
		String binaryHandlerClass = (String) jobDataMap.get(OAIConst.PARAM_BINARYHANDLER);

		String importerClass = (String) jobDataMap.get(OAIConst.PARAM_IMPORTERCLASS);
		
		String oaiIds = (String) jobDataMap.get(OAIConst.PARAM_OAI_IDS);
		
		String[] idArr = (oaiIds != null) ? oaiIds.split(",") : null;


		byte[] xmlData= (byte[]) jobDataMap.get(OAIConst.PARAM_XMLDATA);


		this.context = context;

		if(xmlData!=null){
			return start(xmlData,recordHandlerClass,binaryHandlerClass);
		}
		start(urlImport, oaiBaseUrl, metadataSetId, metadataPrefix, sets, recordHandlerClass,binaryHandlerClass, importerClass,idArr);
		return null;
	}

	private String start(byte[] xmlData, String recordHandlerClass, String binaryHandlerClass){
		try {
			OAIPMHLOMImporter importer = new OAIPMHLOMImporter();
			Constructor<RecordHandlerInterface> recordHandler = null;
			Constructor<BinaryHandler> binaryHandler = null;

			if (recordHandlerClass != null) {
				Class tClass = Class.forName(recordHandlerClass);
				recordHandler = tClass.getConstructor(String.class);
			} else {
				recordHandler = (Constructor) RecordHandlerLOM.class.getConstructor(String.class);
			}
			if (binaryHandlerClass != null) {
				Class tClass = Class.forName(binaryHandlerClass);
				binaryHandler = tClass.getConstructor();
			} else {
				binaryHandler = null;
			}

			logger.info("importer:" + importer.getClass().getName());

			importer.setBinaryHandler(binaryHandler);
			importer.setPersistentHandler(new PersistentHandlerEdusharing(this,importer));
			importer.setRecordHandler(recordHandler);
			importer.setJob(this);
			importer.setSet("xml-import");
			return importer.startImport(xmlData);
		}catch(Throwable t){
			logger.error(t.getMessage(),t);
			return null;
		}
	}

	protected void start(String urlImport, String oaiBaseUrl, String metadataSetId, String metadataPrefix,
			String[] sets, String recordHandlerClass, String binaryHandlerClass, String importerClass, String[] idList) {
		try {
			for(String set : sets) {
				Importer importer = null;
				if (importerClass != null) {
					Class tClass = Class.forName(importerClass);
					Constructor<Importer> constructor = tClass.getConstructor();
					importer = constructor.newInstance();
				} else {
					importer = new OAIPMHLOMImporter();
				}
				try {
					JobHandler.getInstance().updateJobName(context==null ? null : context.getJobDetail(), "Importer Job " + importer.getClass().getSimpleName() + " " + new URL(oaiBaseUrl).getHost());
				} catch (Throwable t) {
				}

				Constructor<RecordHandlerInterface> recordHandler = null;
				Constructor<BinaryHandler> binaryHandler = null;

				if (recordHandlerClass != null) {
					Class tClass = Class.forName(recordHandlerClass);
					recordHandler = tClass.getConstructor(String.class);
				} else {
					recordHandler = (Constructor)RecordHandlerLOM.class.getConstructor(String.class);
				}
				if (binaryHandlerClass != null) {
					Class tClass = Class.forName(binaryHandlerClass);
					binaryHandler = tClass.getConstructor();
				} else {
					binaryHandler = null;
				}

				logger.info("importer:" + importer.getClass().getName());

				importer.setBaseUrl(oaiBaseUrl);
				importer.setBinaryHandler(binaryHandler);
				importer.setMetadataPrefix(metadataPrefix);
				importer.setNrOfRecords(-1);
				importer.setNrOfResumptions(-1);
				importer.setPersistentHandler(new PersistentHandlerEdusharing(this,importer));
				importer.setSet(set);
				importer.setMetadataSetId(metadataSetId);
				importer.setRecordHandler(recordHandler);
				importer.setJob(this);
				if (urlImport != null) {
					RecordHandlerLOM recordHandlerLom = new RecordHandlerLOM(null);
					((OAIPMHLOMImporter) importer).importOAIObjectsFromFile(urlImport, recordHandlerLom);
					new RefreshCacheExecuter().excecute(null, true, null);
					return;
				}

				if (idList != null && idList.length > 0) {
					importer.startImport(idList);
					return;
				}

				long millisec = System.currentTimeMillis();
				logger.info("starting import of set "+set);
				importer.startImport();
				logger.info("finished import in " + (System.currentTimeMillis() - millisec) / 1000 + " secs");
		}
		// refresh cache after importing
		if (!isInterrupted)
			new RefreshCacheExecuter().excecute(null, true, null);

		} catch (Throwable e) {
			logger.error(e.getMessage(),e);
			e.printStackTrace();
		}
	}

	@Override
	public Class[] getJobClasses() {
		return allJobs;
	}
}
