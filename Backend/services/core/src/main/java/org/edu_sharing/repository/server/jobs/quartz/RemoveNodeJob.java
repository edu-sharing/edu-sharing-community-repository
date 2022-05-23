package org.edu_sharing.repository.server.jobs.quartz;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.StoreRef;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.Map;

public class RemoveNodeJob extends AbstractJob {

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        final String id;
        try {
            id = (String) jobExecutionContext.getMergedJobDataMap().get("id");
            if(id==null)
                throw new Exception("id for folder is missing");
        }catch(Throwable t){
            logger.warn(t.getMessage(),t);
            return;
        }
        AuthenticationUtil.runAsSystem(()->{
            String parent=NodeServiceFactory.getLocalService().getPrimaryParent(StoreRef.PROTOCOL_WORKSPACE,StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(),id);
            logger.info("Removing node "+id+" from parent "+parent);
            NodeServiceFactory.getLocalService().removeNode(id,parent);
            logger.info("Removing done");
            return null;
        });
    }

    private void deleteMap(String id) {

    }

    @Override
    public Class[] getJobClasses() {
        return allJobs;
    }
}
