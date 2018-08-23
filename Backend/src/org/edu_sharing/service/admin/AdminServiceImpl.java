package org.edu_sharing.service.admin;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.AuthorityType;
import org.apache.commons.io.input.ReversedLinesFileReader;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.exception.CCException;
import org.edu_sharing.repository.client.rpc.cache.CacheCluster;
import org.edu_sharing.repository.client.rpc.cache.CacheInfo;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.StringTool;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.MCAlfrescoBaseClient;
import org.edu_sharing.repository.server.RepoFactory;
import org.edu_sharing.repository.server.authentication.Context;
import org.edu_sharing.repository.server.importer.ExcelLOMImporter;
import org.edu_sharing.repository.server.importer.OAIPMHLOMImporter;
import org.edu_sharing.repository.server.jobs.quartz.ExporterJob;
import org.edu_sharing.repository.server.jobs.quartz.ImmediateJobListener;
import org.edu_sharing.repository.server.jobs.quartz.JobHandler;
import org.edu_sharing.repository.server.jobs.quartz.JobHandler.JobConfig;
import org.edu_sharing.repository.server.jobs.quartz.OAIConst;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.HttpQueryTool;
import org.edu_sharing.repository.server.tools.NameSpaceTool;
import org.edu_sharing.repository.server.tools.PropertiesHelper;
import org.edu_sharing.repository.server.tools.VCardConverter;
import org.edu_sharing.repository.server.tools.cache.CacheManagerFactory;
import org.edu_sharing.repository.server.tools.cache.EduGroupCache;
import org.edu_sharing.repository.update.ClassificationKWToGeneralKW;
import org.edu_sharing.repository.update.Edu_SharingAuthoritiesUpdate;
import org.edu_sharing.repository.update.Edu_SharingPersonEsuidUpdate;
import org.edu_sharing.repository.update.FixMissingUserstoreNode;
import org.edu_sharing.repository.update.FolderToMap;
import org.edu_sharing.repository.update.KeyGenerator;
import org.edu_sharing.repository.update.Licenses1;
import org.edu_sharing.repository.update.Licenses2;
import org.edu_sharing.repository.update.RefreshMimetypPreview;
import org.edu_sharing.repository.update.Release_1_6_SystemFolderNameRename;
import org.edu_sharing.repository.update.Release_1_7_SubObjectsToFlatObjects;
import org.edu_sharing.repository.update.Release_1_7_UnmountGroupFolders;
import org.edu_sharing.repository.update.Release_3_2_DefaultScope;
import org.edu_sharing.repository.update.Release_3_2_FillOriginalId;
import org.edu_sharing.repository.update.SystemFolderNameToDisplayName;
import org.edu_sharing.repository.update.Update;
import org.edu_sharing.service.admin.model.GlobalGroup;
import org.edu_sharing.service.admin.model.ServerUpdateInfo;
import org.edu_sharing.service.editlock.EditLockServiceFactory;
import org.edu_sharing.service.foldertemplates.FolderTemplatesImpl;
import org.quartz.SchedulerException;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.common.io.Files;

public class AdminServiceImpl implements AdminService  {
	
	private static Logger logger = Logger.getLogger(AdminServiceImpl.class);
	
	@Override
	public ArrayList<String>  getAllValuesFor(String property, HashMap authInfo) throws Throwable {
		ArrayList<String> result = new ArrayList<String>();

		MCAlfrescoBaseClient mcAlfrescoBaseClient = (MCAlfrescoBaseClient) RepoFactory.getInstance(null, authInfo);
		if (mcAlfrescoBaseClient instanceof MCAlfrescoAPIClient) {
			String companyHomeId = mcAlfrescoBaseClient.getCompanyHomeNodeId();
			
			HashMap<String, Object> importFolderProps = mcAlfrescoBaseClient.getChild(companyHomeId, CCConstants.CCM_TYPE_MAP, CCConstants.CM_NAME,
					OAIPMHLOMImporter.FOLDER_NAME_IMPORTED_OBJECTS);
			if (importFolderProps != null) {
				String rootFolderId = (String) importFolderProps.get(CCConstants.SYS_PROP_NODE_UID);
				HashMap<String, HashMap<String, Object>> childs = ((MCAlfrescoAPIClient) mcAlfrescoBaseClient).getChildrenRecursive(rootFolderId,
						CCConstants.CCM_TYPE_IO);
				for (Map.Entry<String, HashMap<String, Object>> childEntry : childs.entrySet()) {
					for (Map.Entry<String, Object> propsEntry : childEntry.getValue().entrySet()) {
						if (propsEntry.getKey().equals(property) && !result.contains((propsEntry.getValue()))) {
							String toAdd = (String) propsEntry.getValue();
							
							if (toAdd != null) {
								result.add(toAdd.trim());
							}

						}
					}
				}
			}

		}

		return result;
	}
	
	@Override
	public void writePublisherToMDSXml(String vcardProps, String valueSpaceProp, String ignoreValues, String filePath, HashMap authInfo) throws Throwable {
		File file = new File(filePath);
		Result result = new StreamResult(file);
		writePublisherToMDSXml(result,vcardProps,valueSpaceProp,ignoreValues,authInfo);
	}
	
	public String getPublisherToMDSXml(List<String> vcardProps, String valueSpaceProp, String ignoreValues, HashMap authInfo) throws Throwable {
		StringWriter writer=new StringWriter();
		Result result = new StreamResult(writer);
		writePublisherToMDSXml(result,StringUtils.join(vcardProps,","),valueSpaceProp,ignoreValues,authInfo);
		return writer.toString();
	}
	
	public void writePublisherToMDSXml(Result result,String vcardProps, String valueSpaceProp, String ignoreValues, HashMap authInfo) throws Throwable {
		
		List<String> ignoreValuesList = null;
		if(ignoreValues != null && !ignoreValues.trim().equals("")){
			ignoreValuesList = Arrays.asList(ignoreValues.split(","));
		}else{
			ignoreValuesList = new ArrayList<String>();
		}
	
		ArrayList<String> allValues = new ArrayList<String>();
		String[] splittedVCardProp = vcardProps.split(",");
		for (String oneVCardProp : splittedVCardProp) {
			allValues.addAll(getAllValuesFor(oneVCardProp, authInfo));
		}
		
		if (allValues != null && allValues.size() > 0) {

			DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
			Document document = documentBuilder.newDocument();
			Element rootElement = document.createElement("valuespaces");
			document.appendChild(rootElement);

			Element valueSpace = document.createElement("valuespace");
			valueSpace.setAttribute("property", valueSpaceProp!=null ? valueSpaceProp : splittedVCardProp[0]);
			rootElement.appendChild(valueSpace);

			ArrayList<String> toSort = new ArrayList<String>();
			for (String vcardString : allValues) {
				
				//multivalue
				String[] splitted = vcardString.split(StringTool.escape(CCConstants.MULTIVALUE_SEPARATOR));
				for(String splittedVCardString :splitted ){
					if(valueSpaceProp==null){
						if(!toSort.contains(splittedVCardString))
							toSort.add(splittedVCardString);
					}
					else{
						ArrayList<HashMap<String, Object>> vcardList = VCardConverter.vcardToHashMap(splittedVCardString);
						if (vcardList != null && vcardList.size() > 0) {
							Map<String, Object> map = vcardList.get(0);
							String publisherString = (String) map.get(CCConstants.VCARD_T_FN);
							
							if (publisherString != null && !publisherString.trim().equals("")) {
								
								//allow only 80 chars cause suggest box makes Problems with longer values
								publisherString = publisherString.trim();
								if(publisherString.length() > 80){
									publisherString = publisherString.substring(0, 80);
								}
								
								//remove "," at the end(appears since we deactivated the punctuation in lucene analyzer)
								if(publisherString.endsWith(",")){
									publisherString = publisherString.substring(0, publisherString.length() - 1);
								}
								
								publisherString = publisherString.trim();
								
								if(!publisherString.equals("") && !toSort.contains(publisherString) && !ignoreValuesList.contains(publisherString.trim())){
									toSort.add(publisherString);
								}
								
							}
						}
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
			
			// Write the DOM document to the file
			Transformer xformer = TransformerFactory.newInstance().newTransformer();
			xformer.transform(source, result);

		}

	}
	
	@Override
	public void refreshApplicationInfo() {
				ApplicationInfoList.refresh();
				RepoFactory.refresh();	
	}
	
	private String getAppPropertiesApplications() throws Exception{
		String appRegistryfileName = "ccapp-registry.properties.xml";
		Properties propsAppRegistry = PropertiesHelper.getProperties(appRegistryfileName, PropertiesHelper.XML);
		return propsAppRegistry.getProperty("applicationfiles");
	}
	
	private void changeAppPropertiesApplications(String newValue,String comment) throws Exception{
		String appRegistryfileName = "ccapp-registry.properties.xml";
		
		Properties propsAppRegistry = PropertiesHelper.getProperties(appRegistryfileName, PropertiesHelper.XML);
		
		String pathAppRegistry = getCatalinaBase()+"/shared/classes/"+appRegistryfileName;
		
		//backup
		propsAppRegistry.storeToXML(new FileOutputStream(new File(pathAppRegistry+System.currentTimeMillis()+".bak")), " backup of registry");
		
		propsAppRegistry.setProperty("applicationfiles", newValue);
		//overwrite
		propsAppRegistry.storeToXML(new FileOutputStream(new File(pathAppRegistry)), comment);
		
	}
	
	@Override
	public void removeApplication(ApplicationInfo info) throws Exception{
		String[] split=getAppPropertiesApplications().split(",");
		List<String> apps=new ArrayList<>(Arrays.asList(split));
		int pos=apps.indexOf(info.getAppFile());
		if(pos==-1)
			throw new Exception("AppInfo file "+info.getAppFile()+" was not found in registry");
		apps.remove(pos);
		String result=org.apache.commons.lang3.StringUtils.join(apps,",");
		changeAppPropertiesApplications(result, new Date()+" removed app: "+info.getAppFile());
		File appFile = new File(getCatalinaBase()+"/shared/classes/"+info.getAppFile());
		appFile.renameTo(new File(appFile.getAbsolutePath()+System.currentTimeMillis()+".bak"));
		ApplicationInfoList.refresh();
		RepoFactory.refresh();
	}
	
	private String getCatalinaBase() throws Exception{
		String catalinaBase = System.getProperty("catalina.base");
		logger.info("catalinaBase:"+catalinaBase);
		if(catalinaBase == null || catalinaBase.trim().equals("")){
			throw new Exception("could not find catalina base in System Properties"); 
		}
		if(catalinaBase.contains("\\")){
			catalinaBase = catalinaBase.replace("\\","/");
		}
		return catalinaBase;
	}
	
	@Override
	public HashMap<String,String> addApplication(String appMetadataUrl) throws Exception{
		
			HttpQueryTool httpQuery = new HttpQueryTool();
			String httpQueryResult = httpQuery.query(appMetadataUrl);
			if(httpQueryResult == null){
				throw new CCException(null,"something went wrong. got no result for metadata url: "+appMetadataUrl);
			}
			
			InputStream is = new ByteArrayInputStream(httpQueryResult.getBytes("UTF-8"));
			return addApplicationFromStream(is);
	}
	
	@Override
	public HashMap<String, String> addApplicationFromStream(InputStream is) throws Exception {
		
		//cause standard properties class does not save the values sorted
		class SortedProperties extends Properties{
					
			public SortedProperties() {
				super();
			}
					
			public SortedProperties(Properties initWith) {
				for (Map.Entry entry : initWith.entrySet()) {
					this.setProperty((String)entry.getKey(), (String)entry.getValue());
				}
			}
					
			//for sorted xml storing
			@Override
				public Set<Object> keySet() {
				return new TreeSet<Object>(super.keySet());
			}

		}
				
		Properties props = new SortedProperties();
		props.loadFromXML(is);
		String appId = props.getProperty(ApplicationInfo.KEY_APPID);
		
		if(appId == null || appId.trim().equals("")){
			throw new Exception("no appId found");
		}
		
		String filename = "app-"+appId+".properties.xml";
		
		//check if appID already exists
		if(ApplicationInfoList.getApplicationInfos().keySet().contains(appId)){
			throw new Exception("appId is already in registry");
		}
		
		//check for mandatory Property type
		String type = props.getProperty(ApplicationInfo.KEY_TYPE);
		if(type == null || type.trim().equals("")){
			throw new Exception("missing type");
		}
		
		if(type.equals(ApplicationInfo.TYPE_RENDERSERVICE)){
			String contentUrl = props.getProperty("contenturl");
			if(contentUrl == null || contentUrl.trim().equals("")){
				throw new Exception("a renderservice must have an contenturl");
			}
			
		}
		
		File appFile = new File(getCatalinaBase()+"/shared/classes/"+filename);
		if(!appFile.exists()){
			props.storeToXML(new FileOutputStream(appFile), "");
		}else{
			throw new Exception("File "+appFile.getPath() + " already exsists");
		}
		
		
		String newProperty=getAppPropertiesApplications()+","+filename;
		changeAppPropertiesApplications(newProperty,new Date()+" added file:"+filename);
		
		
		if(type.equals(ApplicationInfo.TYPE_RENDERSERVICE)){
			
			String contentUrl = props.getProperty("contenturl");
			//String previewUrl = props.getProperty("previewurl");
			
			//store that in the homeApplication.properties.xml cause every repository has it's own renderservice
			//and we don't want to config an renderservice of an remote repository
			
			String homeAppFileName = AdminServiceFactory.HOME_APPLICATION_PROPERTIES;
			Properties homeAppProps = PropertiesHelper.getProperties(homeAppFileName, PropertiesHelper.XML);
			homeAppProps = new SortedProperties(homeAppProps);
			
			String homeAppPath = getCatalinaBase()+"/shared/classes/"+homeAppFileName;
			
			//backup
			homeAppProps.storeToXML(new FileOutputStream(new File(homeAppPath+System.currentTimeMillis()+".bak")), " backup of homeApplication.properties.xml");
			
			homeAppProps.setProperty(ApplicationInfo.KEY_CONTENTURL, contentUrl);
			//homeAppProps.setProperty(ApplicationInfo.KEY_PREVIEWURL, previewUrl);	
			
			//overwrite
			homeAppProps.storeToXML(new FileOutputStream(new File(homeAppPath)), " added contenturl and preview url");
		}
		
		ApplicationInfoList.refresh();
		RepoFactory.refresh();
		
		HashMap<String,String> result = new HashMap<String,String>();
		for(Object key : props.keySet()){
			result.put((String)key,props.getProperty((String)key));
		}
		return result;
	}
	
	@Override
	public String getPropertyToMDSXml(List<String> properties) throws Throwable{
		for(int i=0;i<properties.size();i++)
			properties.set(i, NameSpaceTool.transformToLongQName(properties.get(i)));
		return getPublisherToMDSXml(properties,null, null, getAuthInfo());
	}
	
	@Override
	public ArrayList<ServerUpdateInfo> getServerUpdateInfos(){
		ArrayList<ServerUpdateInfo> result = new ArrayList<ServerUpdateInfo>();			
				result.add(new ServerUpdateInfo(Licenses1.ID,Licenses1.description));
				result.add(new ServerUpdateInfo(Licenses2.ID,Licenses2.description));
				result.add(new ServerUpdateInfo(ClassificationKWToGeneralKW.ID,ClassificationKWToGeneralKW.description));
				result.add(new ServerUpdateInfo(SystemFolderNameToDisplayName.ID,SystemFolderNameToDisplayName.description));
				result.add(new ServerUpdateInfo(Release_1_6_SystemFolderNameRename.ID, Release_1_6_SystemFolderNameRename.description));
				result.add(new ServerUpdateInfo(Release_1_7_SubObjectsToFlatObjects.ID, Release_1_7_SubObjectsToFlatObjects.description));
				result.add(new ServerUpdateInfo(Release_1_7_UnmountGroupFolders.ID, Release_1_7_UnmountGroupFolders.description));
				result.add(new ServerUpdateInfo(Edu_SharingAuthoritiesUpdate.ID, Edu_SharingAuthoritiesUpdate.description));
				result.add(new ServerUpdateInfo(RefreshMimetypPreview.ID,RefreshMimetypPreview.description));
				result.add(new ServerUpdateInfo(FixMissingUserstoreNode.ID,FixMissingUserstoreNode.description));
				result.add(new ServerUpdateInfo(KeyGenerator.ID,KeyGenerator.description));
				result.add(new ServerUpdateInfo(FolderToMap.ID,FolderToMap.description));
				result.add(new ServerUpdateInfo(Edu_SharingPersonEsuidUpdate.ID,Edu_SharingPersonEsuidUpdate.description));
				result.add(new ServerUpdateInfo(Release_3_2_FillOriginalId.ID,Release_3_2_FillOriginalId.description));
				result.add(new ServerUpdateInfo(Release_3_2_DefaultScope.ID,Release_3_2_DefaultScope.description));					
		return result;
	}
	
	@Override
	public String runUpdate(String updateId,boolean execute) throws Exception{
		StringWriter result=new StringWriter();
		PrintWriter out=new PrintWriter(result);
		Update[] avaiableUpdates = new Update[]{new Licenses1(out),new Licenses2(out),new ClassificationKWToGeneralKW(out), new SystemFolderNameToDisplayName(out), new Release_1_6_SystemFolderNameRename(out),new Release_1_7_UnmountGroupFolders(out), new  Edu_SharingAuthoritiesUpdate(out), new Release_1_7_SubObjectsToFlatObjects(out), new RefreshMimetypPreview(out), new KeyGenerator(out), new FixMissingUserstoreNode(out), new FolderToMap(out), new Edu_SharingPersonEsuidUpdate(out), new Release_3_2_FillOriginalId(out), new Release_3_2_DefaultScope(out)};

		ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
		ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
		boolean run=false;
		for(Update update:avaiableUpdates){
			if(update.getId().equals(updateId)){
				if(execute){
					update.execute();
				}else{
					update.test();
				}
				run=true;
			}
		}
		if(!run){
			throw new Exception("Update id "+updateId+" was not found.");
		}
		return result.toString();
	}
	
	@Override
	public void refreshEduGroupCache(boolean keepExisting){
		if(keepExisting) {
			EduGroupCache.refreshByKeepExisting();
		}else{
			EduGroupCache.refresh();
		}
	}
	
	@Override
	public CacheInfo getCacheInfo(String name){
		return CacheManagerFactory.getCacheInfo(name);
	}
	
	@Override
	public List<String> getCatalinaOut() throws IOException{
		List<String> result=new ArrayList<>();
		String path=System.getProperty("catalina.base");
		path+="/logs/catalina.out";
		int n_lines = 1000;
		ReversedLinesFileReader object = new ReversedLinesFileReader(new File(path));
		for(int i=0;i<n_lines;i++){
			String line=object.readLine();
			if(line==null)
				break;
			result.add(line);
		}
		return result;
	}
	
	@Override
	public CacheCluster getCacheCluster(){
		return CacheManagerFactory.getCacheCluster();
	}
	
	/*
	 * Returns the number of active sessions from tomcat
	 */
	@Override
	public int getActiveSessions() throws Exception{
		// https://stackoverflow.com/questions/4069444/getting-a-list-of-active-sessions-in-tomcat-using-java
		String context = Context.getCurrentInstance().getRequest().getSession().getServletContext().getContextPath();
		MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
	    ObjectName objectName = new ObjectName("Catalina:type=Manager,context="+context+",host=localhost");
	    Object activeSessions = mBeanServer.getAttribute(objectName, "activeSessions");
	    System.out.println(activeSessions);
	    return (Integer) activeSessions;	
	}
	
	@Override
	public void applyTemplate(String template,String group,String folderId) throws Throwable{
		FolderTemplatesImpl ft = new FolderTemplatesImpl(new MCAlfrescoAPIClient());
	 	ft.setTemplate(template,group, folderId);
	 	List<String> slist = ft.getMessage();
	 	String error=slist.toString();
	 	if(slist.size()>0 && !error.isEmpty())
	 		throw new Exception(error);
	}
	@Override
	public Collection<NodeRef> getActiveNodeLocks(){
		 return EditLockServiceFactory.getEditLockService().getActiveLocks();
	}
	
	@Override
	public List<GlobalGroup> getGlobalGroups() throws Throwable{
		
		ArrayList<GlobalGroup> result = new ArrayList<GlobalGroup>();
		
		MCAlfrescoBaseClient mcAlfrescoBaseClient = new MCAlfrescoAPIClient();
		HashMap<String, HashMap<String, Object>> raw = mcAlfrescoBaseClient.search("TYPE:cm\\:authorityContainer AND @ccm\\:scopetype:\"global\"");
			
		for(Map.Entry<String, HashMap<String,Object>> entry : raw.entrySet()){
			GlobalGroup group = new GlobalGroup();
			group.setName((String)entry.getValue().get(CCConstants.CM_PROP_AUTHORITY_AUTHORITYNAME));
			group.setDisplayName((String)entry.getValue().get(CCConstants.CM_PROP_AUTHORITY_AUTHORITYDISPLAYNAME));
			group.setNodeId((String)entry.getValue().get(CCConstants.SYS_PROP_NODE_UID));
			group.setAuthorityType(AuthorityType.getAuthorityType(group.getName()).name());
			group.setScope((String)entry.getValue().get(CCConstants.CCM_PROP_SCOPE_TYPE));
			group.setGroupType((String)entry.getValue().get(CCConstants.CCM_PROP_GROUPEXTENSION_GROUPTYPE));
			result.add(group);
		}
		return result;
	}
	
	private HashMap<String, String> getAuthInfo(){
		return new AuthenticationToolAPI().getAuthentication(Context.getCurrentInstance().getRequest().getSession());
	}
	
	@Override
	public List<String> getImporterClasses() throws Exception {
		ArrayList<String> result = new ArrayList<String>();
		List<JobConfig> jcl = JobHandler.getInstance().getJobConfigList();
		Class importerBaseClass = org.edu_sharing.repository.server.jobs.quartz.ImporterJob.class;
		for (JobConfig jc: jcl){
			if(jc.getJobClass().equals(importerBaseClass) || jc.getJobClass().getSuperclass().equals(importerBaseClass)){
				if(!result.contains(jc.getJobClass().getName())) result.add(jc.getJobClass().getName());
			}
		}
		return result;
	}
	
	/**
	 * Import excel data and return the number of rows processed
	 * @param parent
	 * @param csv
	 * @return
	 * @throws Exception 
	 */
	@Override
	public int importExcel(String parent,InputStream csv) throws Exception{
		return new ExcelLOMImporter(parent,csv).getRowCount();
	}
	
	@Override
	public void importOai(String set,String fileUrl, String oaiBaseUrl, String metadataSetId, String metadataPrefix, String importerJobClassName, String importerClassName, String recordHandlerClassName) throws Exception{
	
		//new JobExecuter().start(ImporterJob.class, authInfo, setsParam.toArray(new String[setsParam.size()]));
		
		HashMap<String,Object> paramsMap = new HashMap<String,Object>();
		List<String> sets=new ArrayList<>();
		sets.add(set);
		if(fileUrl!=null && !fileUrl.isEmpty() && !(fileUrl.startsWith("http://") || fileUrl.startsWith("https://")))
				throw new Exception("file url "+fileUrl+" is not a valid url");
		if(fileUrl!=null)
			sets.add(fileUrl);
		paramsMap.put(JobHandler.AUTH_INFO_KEY, getAuthInfo());
		paramsMap.put("sets", sets);
		if(oaiBaseUrl != null && !oaiBaseUrl.trim().equals("")){
			paramsMap.put(OAIConst.PARAM_OAI_BASE_URL, oaiBaseUrl);
		}
		if(metadataSetId != null && !metadataSetId.trim().equals("")){
			paramsMap.put(OAIConst.PARAM_METADATASET_ID, metadataSetId);
		}
		
		//metadataPrefix
		if(metadataPrefix != null && !metadataPrefix.trim().equals("")){
			paramsMap.put(OAIConst.PARAM_OAI_METADATA_PREFIX,metadataPrefix);
		}
		
		if(importerClassName != null && !importerClassName.trim().equals("")){
			paramsMap.put(OAIConst.PARAM_IMPORTERCLASS,importerClassName);
		}
		
		if(recordHandlerClassName != null && !recordHandlerClassName.trim().equals("")){
			paramsMap.put(OAIConst.PARAM_RECORDHANDLER,recordHandlerClassName);
		}
		
		Class importerClass = null;
		for(JobConfig jobConfig : JobHandler.getInstance().getJobConfigList()){
			if(jobConfig.getJobClass().getName().equals(importerJobClassName)){
				importerClass = jobConfig.getJobClass();
			}
		}
		
		if(importerClass == null){
			throw new Exception("no Importer Jobclass found for " + importerJobClassName);
		}
		
		ImmediateJobListener jobListener = JobHandler.getInstance().startJob(importerClass,paramsMap);
		if(jobListener.isVetoed()){
			throw new Exception("job was vetoed by "+jobListener.getVetoBy());
		}
	}
	
	@Override
	public void startCacheRefreshingJob(String folderId,boolean sticky) throws Exception {
		HashMap<String,Object> paramsMap = new HashMap<String,Object>();
		paramsMap.put("rootFolderId", folderId);
		paramsMap.put("sticky", sticky+"");
		paramsMap.put(JobHandler.AUTH_INFO_KEY, getAuthInfo());
		ImmediateJobListener jobListener = JobHandler.getInstance().startJob(org.edu_sharing.repository.server.jobs.quartz.RefreshCacheJob.class, paramsMap);
		
		if(jobListener.isVetoed()){
			throw new Exception("job was vetoed by "+jobListener.getVetoBy());
		}	
	}
	
	@Override
	public void removeDeletedImports(String oaiBaseUrl, String cataloges, String oaiMetadataPrefix) throws Exception{			
		HashMap<String,Object> paramsMap = new HashMap<String,Object>();
		paramsMap.put(JobHandler.AUTH_INFO_KEY, getAuthInfo());
		paramsMap.put(OAIConst.PARAM_OAI_BASE_URL, oaiBaseUrl);
		paramsMap.put(OAIConst.PARAM_OAI_CATALOG_IDS, cataloges);
		paramsMap.put(OAIConst.PARAM_OAI_METADATA_PREFIX, oaiMetadataPrefix);
		
		ImmediateJobListener jobListener = JobHandler.getInstance().startJob(org.edu_sharing.repository.server.jobs.quartz.RemoveDeletedImportsJob.class, paramsMap);
		
		if(jobListener.isVetoed()){
			throw new Exception("job was vetoed by "+jobListener.getVetoBy());
		}
	}
	
	@Override
	public Properties getPropertiesXML(String xmlFile) throws Exception {
		Properties homeAppProps = PropertiesHelper.getProperties(xmlFile, PropertiesHelper.XML);
		return homeAppProps;
	}
	
	@Override
	public void updatePropertiesXML(String xmlFile,Map<String,String> properties) throws Exception {
		File appFile = new File(getCatalinaBase()+"/shared/classes/"+xmlFile);
		Files.copy(appFile, new File(appFile.getAbsolutePath()+System.currentTimeMillis()+".bak"));
		for(String key : properties.keySet()){
			PropertiesHelper.setProperty(key,properties.get(key),xmlFile, PropertiesHelper.XML);
		}
		ApplicationInfoList.refresh();
		RepoFactory.refresh();
	}
	
	public void exportLom(String filterQuery, String targetDir, boolean subobjectHandler) throws Exception {
		HashMap<String,Object> paramsMap = new HashMap<String,Object>();
		paramsMap.put(ExporterJob.PARAM_LUCENE_FILTER, filterQuery);
		paramsMap.put(ExporterJob.PARAM_OUTPUT_DIR, targetDir);
		paramsMap.put(ExporterJob.PARAM_WITH_SUBOBJECTS, new Boolean(subobjectHandler).toString());
		paramsMap.put(JobHandler.AUTH_INFO_KEY, getAuthInfo());
		ImmediateJobListener jobListener = JobHandler.getInstance().startJob(ExporterJob.class,paramsMap);
		if(jobListener.isVetoed()){
			throw new Exception("job was vetoed by "+jobListener.getVetoBy());
		}
	}
	
}
