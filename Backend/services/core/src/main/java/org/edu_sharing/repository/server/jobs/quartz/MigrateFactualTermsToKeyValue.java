package org.edu_sharing.repository.server.jobs.quartz;

import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.importer.PersistenHandlerKeywordsDNBMarc;
import org.edu_sharing.repository.server.jobs.helper.NodeRunner;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;
import org.edu_sharing.service.nodeservice.RecurseMode;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class MigrateFactualTermsToKeyValue extends AbstractJob{

    @JobFieldDescription(description = "startfolder")
    String startFolder;

    @JobFieldDescription(description = "testmode")
    boolean test = true;

    @JobFieldDescription(description = " weather to use the archive store, default is false")
    boolean archive = false;

    static String PROP = CCConstants.CCM_PROP_IO_REPL_CLASSIFICATION_KEYWORD;
    static String PROP_DISPLAY = CCConstants.CCM_PROP_IO_REPL_CLASSIFICATION_KEYWORD_DISPLAY;

    ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
    ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
    BehaviourFilter policyBehaviourFilter = (BehaviourFilter) applicationContext.getBean("policyBehaviourFilter");

    Logger logger = Logger.getLogger(MigrateFactualTermsToKeyValue.class);

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        startFolder = (String)jobExecutionContext.getJobDetail().getJobDataMap().get("startFolder");
        test = new Boolean((String)jobExecutionContext.getJobDetail().getJobDataMap().get("test"));
        if(startFolder == null || startFolder.trim().equals("")) {
            logger.error("no start folder provided");
            return;
        }
        AuthenticationUtil.runAsSystem(() ->{

            StoreRef storeRef = (archive) ? StoreRef.STORE_REF_ARCHIVE_SPACESSTORE : StoreRef.STORE_REF_WORKSPACE_SPACESSTORE;
            run(new NodeRef(storeRef,startFolder));
            return null;
        });
    }

    public void run(NodeRef startFolder){


        PersistenHandlerKeywordsDNBMarc ph = new PersistenHandlerKeywordsDNBMarc();
        NodeRunner nr = new NodeRunner();
        nr.setStartFolder(startFolder.getId());
        nr.setStartFolderStore(startFolder.getStoreRef());
        nr.setTypes(Arrays.asList(CCConstants.CCM_TYPE_IO));
        nr.setRecurseMode(RecurseMode.Folders);
        nr.setInvalidateCache(true);
        nr.setTransaction(NodeRunner.TransactionMode.None);
        nr.setKeepModifiedDate(false);
        nr.setThreaded(false);
        nr.setTask(n-> {

            List<String> displayNames = (List<String>)serviceRegistry.getNodeService().getProperty(n, QName.createQName(PROP));
            if(displayNames != null && displayNames.size() > 0){
                ArrayList<String> keys = new ArrayList<>();
                ArrayList<String> displays = new ArrayList<>();
                for(String displayName : displayNames){

                    List<HashMap<String, Object>> matches = serviceRegistry.getRetryingTransactionHelper().doInTransaction(()-> ph.getEntriesByDisplayValue(displayName));

                    if(matches.size() == 0){
                        logger.error(n+";no keyword matches;" + displayName);
                        continue;
                    }
                    if(matches.size() > 1){
                        List<String> s = new ArrayList<>();
                        matches.stream().forEach(m -> s.add((String)m.get("factual_term_ident")));
                        logger.error(n+";more than one keyword matches;" + displayName+";"+ StringUtils.join(s,","));
                        continue;
                    }
                    String key = (String)matches.get(0).get("factual_term_ident");
                    logger.info(n+";migrate;"+displayName+";"+key);
                    keys.add(key);
                    displays.add(displayName);
                }
                if(keys.size() > 0){
                    if(!test) {
                        serviceRegistry.getRetryingTransactionHelper().doInTransaction(()->{
                            try {
                                policyBehaviourFilter.disableBehaviour(n);
                                serviceRegistry.getNodeService().setProperty(n, QName.createQName(PROP), keys);
                                serviceRegistry.getNodeService().setProperty(n, QName.createQName(PROP_DISPLAY), displays);
                            }finally {
                                policyBehaviourFilter.enableBehaviour(n);
                            }
                            return null;
                        });

                    }
                }
            }
        });
        nr.run();
    }
}
