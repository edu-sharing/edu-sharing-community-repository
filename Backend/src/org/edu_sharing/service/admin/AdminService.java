package org.edu_sharing.service.admin;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.alfresco.service.cmr.repository.NodeRef;
import org.edu_sharing.repository.client.rpc.cache.CacheCluster;
import org.edu_sharing.repository.client.rpc.cache.CacheInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.service.admin.model.GlobalGroup;
import org.edu_sharing.service.admin.model.ServerUpdateInfo;

public interface AdminService {

	void refreshApplicationInfo();

	HashMap<String, String> addApplication(String appMetadataUrl) throws Exception;

	ArrayList<ServerUpdateInfo> getServerUpdateInfos();

	String runUpdate(String updateId, boolean execute) throws Exception;

	

	CacheInfo getCacheInfo(String name);
	
	public CacheCluster getCacheCluster();

	List<GlobalGroup> getGlobalGroups() throws Throwable;

	void importOai(String set, String fileUrl, String oaiBaseUrl, String metadataSetId, String metadataPrefix,
			String importerJobClassName, String importerClassName, String recordHandlerClassName) throws Exception;

	List<String> getImporterClasses() throws Exception;

	void startCacheRefreshingJob(String folderId, boolean sticky) throws Exception;

	void removeDeletedImports(String oaiBaseUrl, String cataloges, String oaiMetadataPrefix) throws Exception;

	String getPropertyToMDSXml(List<String> properties) throws Throwable;

	void writePublisherToMDSXml(String vcardProps, String valueSpaceProp, String ignoreValues, String filePath,
			HashMap authInfo) throws Throwable;

	ArrayList<String> getAllValuesFor(String property, HashMap authInfo) throws Throwable;

	void removeApplication(ApplicationInfo info) throws Exception;

	HashMap<String, String> addApplicationFromStream(InputStream is) throws Exception;

	int importExcel(String parent, InputStream csv) throws Exception;

	Properties getPropertiesXML(String xmlFile) throws Exception;

	void updatePropertiesXML(String xmlFile,Map<String, String> properties) throws Exception;
	
	public void exportLom(String filterQuery,String targetDir, boolean subobjectHandler) throws Exception;

	int getActiveSessions() throws Exception;

	void applyTemplate(String template, String group, String folderId) throws Throwable;

	Collection<NodeRef> getActiveNodeLocks();

	List<String> getCatalinaOut() throws IOException;

	void refreshEduGroupCache(boolean keepExisting);
	
	void startJob(String jobClass, HashMap<String, Object> params) throws Exception;
}
