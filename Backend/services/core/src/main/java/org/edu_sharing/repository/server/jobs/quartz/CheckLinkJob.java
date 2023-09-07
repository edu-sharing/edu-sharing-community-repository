package org.edu_sharing.repository.server.jobs.quartz;

import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
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
import org.edu_sharing.service.usage.Usage2Service;
import org.edu_sharing.service.usage.UsageException;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@JobDescription(description = "Check links from imported objects and do actions for broken links")
public class CheckLinkJob extends AbstractJob {
    public static int STATUS_CODE_INVALID_LINK = 901;
    public static int STATUS_CODE_UNKNOWN = 900;
    @JobFieldDescription(description = "the folder to start from. If not set, defaults to the IMP_OBJ folder")
    private String startFolder;
    @JobFieldDescription(description = "the property that contains the link.", sampleValue = "cclom:location")
    private String property;

    @JobFieldDescription(description = "Minimum count the url must return the same status over multiple runs before it is blocked.", sampleValue = "1")
    private Integer minFailCount;
    @JobFieldDescription(description = "Action to do with broken link objects.")
    private LinkAction action;

    private static enum LinkAction{
        @JobFieldDescription(description = "ccm:locationStatus will be set to response code, nothing more will happen")
        mark,
        @JobFieldDescription(description = "Same as mark but will remove all permissions from the import and mark the imported element as blocked (no reimport)")
        markAndBlock,
        @JobFieldDescription(description = "Same as markAndBlock but will only apply this to elements which have no usages")
        markAndBlockIfNotUsed,
    }
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
        minFailCount = (Integer) jobExecutionContext.getJobDetail().getJobDataMap().get("minFailCount");
        if(minFailCount == null) {
            minFailCount = 1;
        }

        AuthenticationUtil.runAsSystem(()->{
            execute(startFolder,property);
            return null;
        });
    }

    private void execute(String startFolder, String property){
        ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
        ServiceRegistry serviceregistry = (ServiceRegistry)applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
        NodeService nodeService = serviceregistry.getNodeService();

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
                StatusResult status = getStatus(location);
                int failCount = 0;
                if(shouldBlockStatus(status)) {
                    logger.warn(ref.getId() + ";" + location);
                    if(Objects.equals(nodeService.getProperty(ref, QName.createQName(CCConstants.CCM_PROP_IO_LOCATION_STATUS_FAIL_COUNT)), status.getStatus())) {
                        // increase fail count
                        Integer count = (Integer) nodeService.getProperty(ref, QName.createQName(CCConstants.CCM_PROP_IO_LOCATION_STATUS_FAIL_COUNT));
                        if(count == null) {
                            count = 0;
                        }
                        failCount = count + 1;
                        nodeService.setProperty(ref, QName.createQName(CCConstants.CCM_PROP_IO_LOCATION_STATUS_FAIL_COUNT), failCount);

                    } else {
                        // reset fail count
                        nodeService.removeProperty(ref, QName.createQName(CCConstants.CCM_PROP_IO_LOCATION_STATUS_FAIL_COUNT));
                    }
                }
                nodeService.setProperty(ref, QName.createQName(CCConstants.CCM_PROP_IO_LOCATION_STATUS), status.getStatus());
                if(failCount >= minFailCount && shouldBlockStatus(status) && (
                        action.equals(LinkAction.markAndBlock) || (
                        action.equals(LinkAction.markAndBlockIfNotUsed) && !hasUsages(ref)
                        )
                )
                ){
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

    private boolean hasUsages(NodeRef ref) {
        try {
            return !new Usage2Service().getUsageByParentNodeId(null, null, ref.getId()).isEmpty();
        } catch (UsageException e) {
            logger.warn("Could not resolve usages for " + ref, e);
            return true;
        }
    }

    private boolean shouldBlockStatus(StatusResult status) {
        return status.getStatus() != STATUS_CODE_UNKNOWN && status.getStatus() != STATUS_CODE_INVALID_LINK && status.getStatus() >= 400;
    }

    StatusResult getStatus(String location) {
        try {
            StatusLine sl = makeHttpCall(location);
            int status = sl.getStatusCode();
            return new StatusResult(status, null);
        } catch (ClientProtocolException e) {
            return new StatusResult(STATUS_CODE_INVALID_LINK, e);
            //logger.error(ref.getId()+";" + replicationSourceId + ";"+e.getMessage());
        }  catch (IOException e) {
            return new StatusResult(404, e);
            //logger.error(ref.getId()+";" + replicationSourceId + ";"+e.getMessage());
        } catch(Throwable t) {
            return new StatusResult(STATUS_CODE_UNKNOWN, t);
        }
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

    public static class StatusResult {
        private int status;
        private Throwable exception;

        public StatusResult(int status, Throwable exception) {
            this.status = status;
            this.exception = exception;
        }


        public int getStatus() {
            return status;
        }

        public Throwable getException() {
            return exception;
        }
    }
}
