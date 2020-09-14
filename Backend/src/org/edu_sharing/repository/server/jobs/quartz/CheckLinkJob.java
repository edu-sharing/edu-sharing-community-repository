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
import org.edu_sharing.repository.server.jobs.helper.NodeRunner;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class CheckLinkJob extends AbstractJob {

    ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
    ServiceRegistry serviceregistry = (ServiceRegistry)applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
    NodeService nodeService = serviceregistry.getNodeService();
    Repository repositoryHelper = (Repository) applicationContext.getBean("repositoryHelper");

    Logger logger = Logger.getLogger(CheckLinkJob.class);

    public static String PARAM_START_FOLDER = "startFolder";
    public static String PARAM_PROPERTY = "property";

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {

        String startfolder = (String)jobExecutionContext.getJobDetail().getJobDataMap().get(PARAM_START_FOLDER);
        String property = (String)jobExecutionContext.getJobDetail().getJobDataMap().get(PARAM_PROPERTY);
        if(property == null){
            property =  CCConstants.LOM_PROP_TECHNICAL_LOCATION;
        } else {
            property = CCConstants.getValidGlobalName(property);
        }
        String fprop = property;
        AuthenticationUtil.runAsSystem(()->{
            execute(startfolder,fprop);
            return null;
        });
    }

    private void execute(String startFolder, String property){

        NodeRunner runner = new NodeRunner();
        runner.setTask((ref)->{

            String replicationSourceId = (String) nodeService.getProperty(ref, QName.createQName(CCConstants.CCM_PROP_IO_REPLICATIONSOURCEID));
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
                    if(status != 200) {
                        //logger.info(ref.getId() + ";" + replicationSourceId + ";" + status);
                    }
                } catch (IOException e) {
                    //logger.error(ref.getId()+";" + replicationSourceId + ";"+e.getMessage());
                }
                nodeService.setProperty(ref, QName.createQName(CCConstants.CCM_PROP_IO_LOCATION_STATUS), status);
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
