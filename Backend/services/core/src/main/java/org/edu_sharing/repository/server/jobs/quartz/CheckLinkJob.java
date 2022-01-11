package org.edu_sharing.repository.server.jobs.quartz;

import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.apache.http.StatusLine;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.importer.OAIPMHLOMImporter;
import org.edu_sharing.repository.server.jobs.helper.NodeRunner;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;
import org.edu_sharing.service.admin.AdminServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@JobDescription(description = "Check links from imported objects and do actions for broken links")
public class CheckLinkJob extends AbstractJob {
    @JobFieldDescription(description = "the folder to start from. If not set, defaults to the IMP_OBJ folder")
    private String startFolder;
    @JobFieldDescription(description = "the property that contains the link.", sampleValue = "cclom:location")
    private String property;

    @JobFieldDescription(description = "Action to do with broken link objects.")
    private LinkAction action;

    private static enum LinkAction{
        @JobFieldDescription(description = "ccm:locationStatus will be set to response code, nothing more will happen")
        mark,
        @JobFieldDescription(description = "Same as mark but will remove all permissions from the import and mark the imported element as blocked (no reimport)")
        markAndBlock,
    }

    ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
    ServiceRegistry serviceregistry = (ServiceRegistry)applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
    NodeService nodeService = serviceregistry.getNodeService();

    Logger logger = Logger.getLogger(CheckLinkJob.class);

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {

        startFolder = (String)jobExecutionContext.getJobDetail().getJobDataMap().get("startFolder");
        if(startFolder == null){
            startFolder = AuthenticationUtil.runAsSystem(() ->
             NodeServiceHelper.getNodeInCompanyNode(OAIPMHLOMImporter.FOLDER_NAME_IMPORTED_OBJECTS).getId()
            );
        }
        property = (String)jobExecutionContext.getJobDetail().getJobDataMap().get("property");
        if(property == null){
            property =  CCConstants.LOM_PROP_TECHNICAL_LOCATION;
        } else {
            property = CCConstants.getValidGlobalName(property);
        }
        action = LinkAction.valueOf((String) jobExecutionContext.getJobDetail().getJobDataMap().get("action"));

        AuthenticationUtil.runAsSystem(()->{
            execute(startFolder,property);
            return null;
        });
    }

    private void execute(String startFolder, String property){

        NodeRunner runner = new NodeRunner();
        runner.setTask((ref)->{

            Object value = nodeService.getProperty(ref,QName.createQName(property));
            if(value == null){
                return;
            }
            String location = null;
            if(value instanceof List){
                location = (String) ((List)value).get(0);
            }else{
                location = value.toString();
            }
            if(location != null && location.startsWith("http")){
                int status = 0;
                try {
                    StatusLine sl = makeHttpCall(location);
                    status = sl.getStatusCode();
                } catch (IOException e) {
                    status = 404;
                    //logger.error(ref.getId()+";" + replicationSourceId + ";"+e.getMessage());
                }
                if(status >= 300) {
                    logger.info(ref.getId() + ";" + location + ";" + status);
                }
                nodeService.setProperty(ref, QName.createQName(CCConstants.CCM_PROP_IO_LOCATION_STATUS), status);
                if(action.equals(LinkAction.markAndBlock) && status>=300){
                    try {
                        NodeServiceHelper.blockImport(ref);
                    } catch (Exception e) {
                        logger.warn("Could not block node " + ref + " for import", e);
                    }
                }
            }
        });

        runner.setTypes(Collections.singletonList(CCConstants.CCM_TYPE_IO));
        if(startFolder != null) {
            runner.setStartFolder(startFolder);
        }
        runner.setRunAsSystem(true);
        runner.setKeepModifiedDate(true);
        runner.setTransaction(NodeRunner.TransactionMode.Local);
        runner.setThreaded(false);

        int processNodes = runner.run();
    }

    StatusLine makeHttpCall(String link) throws IOException {
        int timeout = 10;
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(timeout * 1000)
                .setConnectionRequestTimeout(timeout * 1000)
                .setSocketTimeout(timeout * 1000)
                .setRedirectsEnabled(true)
                .build();
        CloseableHttpClient client =
                HttpClientBuilder.create().setDefaultRequestConfig(config).build();

        try {

            HttpGet httpGet = new HttpGet(link);
            CloseableHttpResponse resp = client.execute(httpGet);
            return resp.getStatusLine();
        }finally{
            client.close();
        }

    }

    @Override
    public Class[] getJobClasses() {
        this.addJobClass(CheckLinkJob.class);
        return allJobs;
    }
}
