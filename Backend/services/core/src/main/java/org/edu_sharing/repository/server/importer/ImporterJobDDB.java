package org.edu_sharing.repository.server.importer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.LogFactory;
import org.edu_sharing.metadataset.v2.MetadataSet;
import org.edu_sharing.metadataset.v2.tools.MetadataHelper;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.appcontext.AppContextScope;
import org.edu_sharing.repository.server.jobs.quartz.AbstractJob;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.model.NodeRef;
import org.edu_sharing.service.nodeservice.NodeServiceDDBImpl;
import org.edu_sharing.service.search.SearchServiceDDBImpl;
import org.edu_sharing.service.search.model.SearchToken;
import org.edu_sharing.spring.ApplicationContextFactory;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
public class ImporterJobDDB extends AbstractJob {


    @Autowired
    private SearchServiceDDBImpl searchServiceDDBImpl;
	
	public ImporterJobDDB() {
		this.logger = LogFactory.getLog(ImporterJobDDB.class);
	}
	
	
	public void execute(org.quartz.JobExecutionContext context) throws org.quartz.JobExecutionException {
		Map<String, Object> jobDataMap = context.getMergedJobDataMap();
		
		
		String ddbFile = (String)jobDataMap.get("applicationfile");
		String query  = (String)jobDataMap.get("query");

        try{
			start(ddbFile,query);
		}catch(Throwable e){
			throw new JobExecutionException(e);
		}
	};
	
	void start(String ddbFile, String query) throws Throwable{
        try(AppContextScope.UseApplicationInfo useApplicationInfo = AppContextScope.useApplicationInfo(ApplicationInfoList.getRepositoryInfoByRepositoryType(ApplicationInfo.REPOSITORY_TYPE_DDB))) {
            ApplicationInfo appInfo = ApplicationInfoList.getRepositoryInfo(ddbFile);
            ApplicationInfo app = useApplicationInfo.getAppInfo();
            MetadataSet mds = MetadataHelper.getMetadataset(app, CCConstants.metadatasetdefault_id);

            SearchToken token = new SearchToken();
            Map<String, String[]> criterias = new HashMap<>();
            criterias.put(MetadataSet.DEFAULT_CLIENT_QUERY_CRITERIA, new String[]{query});
            List<NodeRef> ddbObjects = searchServiceDDBImpl.search(mds, MetadataSet.DEFAULT_CLIENT_QUERY, criterias, token).getData();

            for (NodeRef node : ddbObjects) {
                NodeServiceDDBImpl nodeServiceDDB = ApplicationContextFactory.getApplicationContext().getBean(NodeServiceDDBImpl.class);

                Map<String, Object> ddbObject = nodeServiceDDB.getProperties(node.getStoreProtocol(), node.getStoreId(), node.getNodeId());

                ddbObject.put(CCConstants.CCM_PROP_IO_REPLICATIONSOURCEID, ddbObject.get(CCConstants.SYS_PROP_NODE_UID));

                // clear the sys prop node id cause it will be created by alfresco
                ddbObject.remove(CCConstants.SYS_PROP_NODE_UID);

                // clear virtual property
                ddbObject.remove(CCConstants.CM_ASSOC_THUMBNAILS);

                ddbObject.put(CCConstants.CCM_PROP_IO_REPLICATIONSOURCE, appInfo.getAppId());
                //ddbObject.put(CCConstants.CCM_PROP_IO_REPLICATIONSOURCETIMESTAMP, appInfo.getAppId());


                String name = (String) ddbObject.get(CCConstants.LOM_PROP_GENERAL_TITLE);
                name = name.replaceAll(ApplicationInfoList.getHomeRepository().getValidatorRegexCMName(), "_");

                //replace the ending dot with nothing
                name = name.replaceAll("[.]*$", "");

                ddbObject.put(CCConstants.CM_NAME, name);

                String nodeId = new PersistentHandlerEdusharing(this, null, true).safe(new RecordHandlerStatic(ddbObject), "0", "ddb");
                new MCAlfrescoAPIClient().createVersion(nodeId);

            }
        }
	}

	
	
	@Override
	public Class<?>[] getJobClasses() {
		// TODO Auto-generated method stub
        ArrayList<Class<?>> jobs = new ArrayList<>(Arrays.asList(allJobs));
		jobs.add(ImporterJobDDB.class);		
		return jobs.toArray(new Class[0]);
		
	}
}
