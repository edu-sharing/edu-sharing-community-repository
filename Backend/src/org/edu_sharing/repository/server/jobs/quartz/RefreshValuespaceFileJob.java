package org.edu_sharing.repository.server.jobs.quartz;

import java.io.File;
import java.io.FileInputStream;
import java.io.UnsupportedEncodingException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.alfresco.repo.search.impl.lucene.SolrJSONResultSet;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchParameters.FieldFacet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.util.Pair;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetValueKatalog;
import org.edu_sharing.repository.server.AuthenticationTool;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.RepoFactory;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.metadataset.SAXValueSpaceHandler;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class RefreshValuespaceFileJob extends AbstractJob {

	public static final String CONFIG_IGNORE_ENTRIES_KEY = "ignore_entries";
	
	public static final String CONFIG_VALUESPACE_PROP = "valuespaceprop";
	
	public static final String CONFIG_FILEPATH = "filepath";
	
	public static final String CONFIG_PROPERTIES = "properties";
	
	public static final String CONFIG_KEEP_EXISTING_VALUES ="keep_existing_values";
	
	Logger logger = Logger.getLogger(RefreshValuespaceFileJob.class);
	
	@Override
	public void execute(JobExecutionContext jobContext) throws JobExecutionException {
		logger.info("starting");
		JobDataMap jobDataMap = jobContext.getJobDetail().getJobDataMap();

		String ignoreEntries = (String)jobDataMap.get(CONFIG_IGNORE_ENTRIES_KEY);
		String props = (String)jobDataMap.get(CONFIG_PROPERTIES);
		String valuespaceProp = (String)jobDataMap.get(CONFIG_VALUESPACE_PROP);
		
		//String filePath = "tomcat/shared/classes/org/edu_sharing/metadataset/valuespace_learnline2_0_contributer.xml";
		String filePath = (String)jobDataMap.get(CONFIG_FILEPATH);
		
		String keepExistingValues = (String)jobDataMap.get(CONFIG_KEEP_EXISTING_VALUES);
		
		if(props == null || props.trim().equals("")){
			throw new JobExecutionException(CONFIG_PROPERTIES + " is a mandatory!");
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
			
			this.writeToValueSpaceFile(props, valuespaceProp, ignoreEntries, filePath, authInfo,new Boolean(keepExistingValues));
			
			RepoFactory.refreshMetadataSets();
			
		}catch(org.alfresco.webservice.authentication.AuthenticationFault e){
			logger.error("AuthenticationFault while excecuting RefreshPublisherListJob:"+e.getMessage());
			throw new JobExecutionException(e);
		}catch(Throwable e){
			logger.error("Throwable while excecuting RefreshPublisherListJob"+e.getMessage());
			throw new JobExecutionException(e);
		}
		logger.info("returning");
	}
	
	public void writeToValueSpaceFile(String props, String valuespaceProp, String ignoreEntries, String filePath, HashMap<String,String> authinfo, boolean keepExisting){
		ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
		ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
		
		SearchService searchService = (SearchService)serviceRegistry.getSearchService();
		SearchParameters sp = new SearchParameters();
		sp.addStore(MCAlfrescoAPIClient.storeRef);
		sp.setLanguage(SearchService.LANGUAGE_LUCENE);
		sp.setQuery("TYPE:\"{http://www.campuscontent.de/model/1.0}io\"");
		//sp.setSkipCount(0);
		String[] facetteProps = props.split(",");
		for(String facetteProp : facetteProps){
			FieldFacet fieldFacet = new FieldFacet("@"+facetteProp);
			fieldFacet.setLimit(100000);
			fieldFacet.setMinCount(0);
			sp.addFieldFacet(fieldFacet);
		}
		try{
			logger.info("start solr query");
			SolrJSONResultSet rs = (SolrJSONResultSet)searchService.query(sp);
			logger.info("finished solr query");
			
			DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
			Document document = documentBuilder.newDocument();
			Element rootElement = document.createElement("valuespaces");
			document.appendChild(rootElement);

			Element valueSpace = document.createElement("valuespace");
			valueSpace.setAttribute("ignoreI18n","true");
			valueSpace.setAttribute("property", valuespaceProp);
			rootElement.appendChild(valueSpace);
			
			ArrayList<String> toSort = new ArrayList<String>();
			
			if(keepExisting){
				try{
					logger.info("start keep existing");
					
					File file = new File(filePath);
					List<MetadataSetValueKatalog> cata = new SAXValueSpaceHandler(new FileInputStream(file), null, null, valuespaceProp).getResult();
					for(MetadataSetValueKatalog entry : cata){
						if(!toSort.contains(entry.getKey())){
							toSort.add(entry.getKey());
						}
					}
					logger.info("finish keep existing");
				}catch(Exception e){
					logger.error(e.getMessage(),e);
				}
			}
			
			for(String facetteProp : facetteProps){
				List<Pair<String, Integer>> facettes = rs.getFieldFacet("@"+facetteProp);
				for(Pair<String, Integer> pair : facettes){
					try{
						String first = new String(pair.getFirst().replaceAll("\\{[a-z]*\\}", "").getBytes(),"UTF-8");
						
						if(first != null && !first.trim().equals("") && !toSort.contains(first.trim())){
							toSort.add(first.trim());
						}
					}catch(UnsupportedEncodingException e){
						logger.error(e.getMessage(), e);
					}
				}
			}
			
			Comparator<String> comp = new Comparator<String>() {
				public int compare(String o1, String o2) {
					Collator collator = Collator.getInstance(Locale.GERMAN);		
					return collator.compare(o1, o2);
				}
			};
			Collections.sort(toSort, comp);
			
			
			for (String sortedVal : toSort) {
				Element key = document.createElement("key");
				key.appendChild(document.createTextNode(sortedVal));
				valueSpace.appendChild(key);
			}
			
			
			Source source = new DOMSource(document);

			// Prepare the output file
			File file = new File(filePath);
			Result result = new StreamResult(file);

			// Write the DOM document to the file
			logger.info("start writing "+toSort.size()+" values in valuespacefile "+filePath);
			Transformer xformer = TransformerFactory.newInstance().newTransformer();
			xformer.transform(source, result);
			logger.info("finished writing values in valuespacefile");
			
		}catch(ClassCastException e){
			logger.error(e.getMessage(), e);
		}catch(ParserConfigurationException e){
			logger.error(e.getMessage(), e);
		}catch(TransformerConfigurationException e){
			logger.error(e.getMessage(), e);
		}catch(TransformerException e){
			logger.error(e.getMessage(), e);
		}
		
	}
	
	

	@Override
	public Class[] getJobClasses() {
		// TODO Auto-generated method stub
		return allJobs;
	}

}
