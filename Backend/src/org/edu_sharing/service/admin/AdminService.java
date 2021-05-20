package org.edu_sharing.service.admin;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.alfresco.service.cmr.repository.NodeRef;
import org.edu_sharing.repository.client.rpc.cache.CacheCluster;
import org.edu_sharing.repository.client.rpc.cache.CacheInfo;
import org.edu_sharing.repository.server.jobs.quartz.JobDescription;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.service.admin.model.GlobalGroup;
import org.edu_sharing.repository.server.jobs.quartz.JobInfo;
import org.edu_sharing.service.admin.model.RepositoryConfig;
import org.edu_sharing.service.admin.model.ServerUpdateInfo;
import org.edu_sharing.service.admin.model.ToolPermission;

public interface AdminService {

    List<JobInfo> getJobs() throws Throwable;

	void cancelJob(String jobName) throws Throwable;

	void refreshApplicationInfo();

	HashMap<String, String> addApplication(String appMetadataUrl) throws Exception;

	List<ServerUpdateInfo> getServerUpdateInfos();

	String runUpdate(String updateId, boolean execute) throws Exception;



	CacheInfo getCacheInfo(String name);

    Map<Serializable,Serializable> getCacheEntries(String beanName);

    public void removeCacheEntry(Integer index, String beanName);

	public CacheCluster getCacheCluster();

	public List<CacheCluster> getCacheClusters();


	List<GlobalGroup> getGlobalGroups() throws Throwable;

	void importOai(String set, String fileUrl, String oaiBaseUrl, String metadataSetId, String metadataPrefix,
                   String importerJobClassName, String importerClassName, String recordHandlerClassName, String binaryHandlerClassName, String persistentHandlerClassName,
				   String oaiIds, boolean forceUpdate, String from, String until, String periodInDays) throws Exception;

	List<Class> getImporterClasses() throws Exception;

	void startCacheRefreshingJob(String folderId, boolean sticky) throws Exception;

	void removeDeletedImports(String oaiBaseUrl, String cataloges, String oaiMetadataPrefix) throws Exception;

	String getPropertyToMDSXml(List<String> properties) throws Throwable;

	void writePublisherToMDSXml(String vcardProps, String valueSpaceProp, String ignoreValues, String filePath,
			HashMap authInfo) throws Throwable;

	ArrayList<String> getAllValuesFor(String property, HashMap authInfo) throws Throwable;

	void removeApplication(ApplicationInfo info) throws Exception;

	HashMap<String, String> addApplicationFromStream(InputStream is) throws Exception;

	int importExcel(String parent, InputStream csv, Boolean addToCollection) throws Exception;

	Properties getPropertiesXML(String xmlFile) throws Exception;

	void updatePropertiesXML(String xmlFile,Map<String, String> properties) throws Exception;
	
	public void exportLom(String filterQuery,String targetDir, boolean subobjectHandler) throws Exception;

	int getActiveSessions() throws Exception;

	Collection<NodeRef> getActiveNodeLocks();

	void applyTemplate(String template, String group, String folderId) throws Throwable;

	void clearCache(String beanName);

	List<String> getCatalinaOut() throws IOException;

	int importCollections(String parent, InputStream is) throws Throwable;

	String uploadTemp(String name, InputStream is) throws Exception;

	void startJob(String jobClass, HashMap<String, Object> params) throws Exception;

    RepositoryConfig getConfig();

    void setConfig(RepositoryConfig config);

    Map<String, ToolPermission> getToolpermissions(String authority) throws Throwable;

    String addToolpermission(String name) throws Throwable;

    void setToolpermissions(String authority,
                            Map<String, ToolPermission.Status> toolpermissions) throws Throwable;

	void refreshEduGroupCache(boolean keepExisting);

    void testMail(String receiver, String template);

    String importOaiXml(InputStream xml, String recordHandlerClassName, String binaryHandlerClassName) throws Exception;

    void updateConfigFile(String filename,String content) throws Throwable;
	String getConfigFile(String filename) throws Throwable;

	List<JobDescription> getJobDescriptions();
}
