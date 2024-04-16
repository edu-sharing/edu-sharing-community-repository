package org.edu_sharing.repository.server.jobs.quartz;

import org.alfresco.repo.node.db.DbNodeServiceImpl;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ForceDeleteJob extends AbstractJob {

    public static final String PARAM_STORE = "store";
    public static final String PARAM_PROTOCOL = "protocol";
    public static final String PARAM_ID = "id";


    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {

        AuthenticationUtil.runAsSystem(() -> {
            String store = (String)jobExecutionContext.getJobDetail().getJobDataMap().get(PARAM_STORE);
            String protocol = (String)jobExecutionContext.getJobDetail().getJobDataMap().get(PARAM_PROTOCOL);
            String id = (String)jobExecutionContext.getJobDetail().getJobDataMap().get(PARAM_ID);
            NodeRef nodeRef  = new NodeRef(new StoreRef(protocol,store),id);

            ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
            ServiceRegistry sr = (ServiceRegistry)applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
            sr.getRetryingTransactionHelper().doInTransaction(()-> {
                NodeServiceFactory.getLocalService().removeNodeForce(store, protocol, id,false);
                return null;
            });
            return null;
        });
    }

    @Override
    public Class[] getJobClasses() {
        return new Class[0];
    }
}
