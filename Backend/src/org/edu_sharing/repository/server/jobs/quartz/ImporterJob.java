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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.LogFactory;
import org.edu_sharing.repository.server.importer.Importer;
import org.edu_sharing.repository.server.importer.OAIPMHLOMImporter;
import org.edu_sharing.repository.server.importer.PersistentHandlerEdusharing;
import org.edu_sharing.repository.server.importer.RecordHandlerInterface;
import org.edu_sharing.repository.server.importer.RecordHandlerLOM;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class ImporterJob extends AbstractJob {

	public ImporterJob() {
		this.logger = LogFactory.getLog(ImporterJob.class);
	}

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		
		logger.info("starting");
		Map jobDataMap = context.getJobDetail().getJobDataMap();

		Object setsParamObj = jobDataMap.get(OAIConst.PARAM_OAI_SETS);
		// String setsParam = (String) jobDataMap.get("sets");
		List<String> splitted = null;
		if (setsParamObj instanceof String) {
			splitted = new ArrayList(Arrays.asList(((String) setsParamObj).split(",")));
		} else {
			splitted = (List) setsParamObj;
		}

		String urlImport = null;

		for (String set : splitted) {
			if (set.contains("http://")) {
				urlImport = set.trim();
			}
		}
		if (urlImport != null) {
			splitted.remove(urlImport);
		}

		String[] sets = splitted.toArray(new String[splitted.size()]);
		String oaiBaseUrl = (String) jobDataMap.get(OAIConst.PARAM_OAI_BASE_URL);
		String metadataPrefix = (String) jobDataMap.get(OAIConst.PARAM_OAI_METADATA_PREFIX);
		metadataPrefix = (metadataPrefix == null || metadataPrefix.trim().equals("")) ? "oai_lom-de" : metadataPrefix;
		String metadataSetId = (String) jobDataMap.get(OAIConst.PARAM_METADATASET_ID);

		String recordHandlerClass = (String) jobDataMap.get(OAIConst.PARAM_RECORDHANDLER);

		String importerClass = (String) jobDataMap.get(OAIConst.PARAM_IMPORTERCLASS);
		start(urlImport, oaiBaseUrl, metadataSetId, metadataPrefix, sets, recordHandlerClass, importerClass);
		logger.info("returns");
	}

	protected void start(String urlImport, String oaiBaseUrl, String metadataSetId, String metadataPrefix,
			String[] sets, String recordHandlerClass, String importerClass) {
		try {

			Importer importer = null;
			if (importerClass != null) {
				Class tClass = Class.forName(importerClass);
				Constructor constructor = tClass.getConstructor();
				importer = (Importer) constructor.newInstance();
			} else {
				importer = new OAIPMHLOMImporter();
			}

			RecordHandlerInterface recordHandler = null;

			if (recordHandlerClass != null) {
				Class tClass = Class.forName(recordHandlerClass);
				Constructor constructor = tClass.getConstructor(String.class);
				recordHandler = (RecordHandlerInterface) constructor.newInstance(metadataSetId);
			} else {
				recordHandler = new RecordHandlerLOM(metadataSetId);
			}
			
			logger.info("importer:" + importer.getClass().getName());
			logger.info("recordHandler:" + recordHandler.getClass().getName());
			
			importer.setBaseUrl(oaiBaseUrl);
			importer.setBinaryHandler(null);
			importer.setMetadataPrefix(metadataPrefix);
			importer.setNrOfRecords(-1);
			importer.setNrOfResumptions(-1);
			importer.setPersistentHandler(new PersistentHandlerEdusharing());
			importer.setSet(sets[0]);
			importer.setRecordHandler(recordHandler);

			if (urlImport != null) {
				RecordHandlerLOM recordHandlerLom = new RecordHandlerLOM(null);
				((OAIPMHLOMImporter)importer).importOAIObjectsFromFile(urlImport, recordHandlerLom);
				new RefreshCacheExecuter().excecute(null, true, null);
				return;
			}

			long millisec = System.currentTimeMillis();
			logger.info("starting import");
			importer.startImport();
			logger.info("finished import in " + (System.currentTimeMillis() - millisec) / 1000 + " secs");

			// refresh cache after importing
			new RefreshCacheExecuter().excecute(null, true, null);

		} catch (Throwable e) {
			logger.error(e.getMessage(), e);
			e.printStackTrace();
		}
	}

	@Override
	public Class[] getJobClasses() {
		return allJobs;
	}
}
