package org.edu_sharing.repository.server.jobs.quartz;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.DuplicateChildNodeNameException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.jobs.helper.NodeRunner;
import org.edu_sharing.repository.server.tools.I18nServer;
import org.edu_sharing.repository.server.tools.cache.RepositoryCache;
import org.edu_sharing.service.nodeservice.RecurseMode;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

import java.util.Arrays;
import java.util.function.Consumer;


public class RenamePersonHomeFolders extends AbstractJob{

    public static final String PARAM_EXECUTE = "EXECUTE";
    public static final String DESCRIPTION = "Rename folders in person homedir i.e when system language changes.";

    ApplicationContext appContext = AlfAppContextGate.getApplicationContext();
    ServiceRegistry serviceRegistry = (ServiceRegistry)appContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
    Repository repo = (Repository)appContext.getBean("repositoryHelper");

    Logger logger = Logger.getLogger(RenamePersonHomeFolders.class);


    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        boolean execute = new Boolean((String)jobExecutionContext.getJobDetail().getJobDataMap().get(PARAM_EXECUTE));
        NodeRunner nr = new NodeRunner();
        nr.setTypes(Arrays.asList(new String[]{CCConstants.CCM_TYPE_MAP}));
        nr.setStartFolder(repo.getCompanyHome().getId());
        nr.setRunAsSystem(true);
        nr.setKeepModifiedDate(true);
        nr.setTransaction(NodeRunner.TransactionMode.Local);
        nr.setThreaded(false);
        nr.setTask((NodeRef nodeRef) -> {
            String mapType = (String)serviceRegistry.getNodeService().getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_MAP_TYPE));
            if(mapType == null) return;
            if(CCConstants.CCM_VALUE_MAP_TYPE_DOCUMENTS.equals(mapType)){
                rename(nodeRef,CCConstants.CCM_VALUE_MAP_TYPE_DOCUMENTS, CCConstants.I18n_USERFOLDER_DOCUMENTS, execute);
            } else if(CCConstants.CCM_VALUE_MAP_TYPE_EDUGROUP.equals(mapType)){
                rename(nodeRef,CCConstants.CCM_VALUE_MAP_TYPE_EDUGROUP, CCConstants.I18n_USERFOLDER_GROUPS, execute);
            } else if(CCConstants.CCM_VALUE_MAP_TYPE_FAVORITE.equals(mapType)){
                rename(nodeRef,CCConstants.CCM_VALUE_MAP_TYPE_FAVORITE, CCConstants.I18n_USERFOLDER_FAVORITES, execute);
            }else if(CCConstants.CCM_VALUE_MAP_TYPE_IMAGES.equals(mapType)){
                rename(nodeRef,CCConstants.CCM_VALUE_MAP_TYPE_IMAGES, CCConstants.I18n_USERFOLDER_IMAGES, execute);
            }
        });

        nr.run();
    }

    private void rename(NodeRef nodeRef, String mapType, String i18Key, boolean execute){
        String currentName = (String)serviceRegistry.getNodeService().getProperty(nodeRef, ContentModel.PROP_NAME);
        String scope = (String)serviceRegistry.getNodeService().getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_EDUSCOPE_NAME));
        String creator = (String)serviceRegistry.getNodeService().getProperty(nodeRef, ContentModel.PROP_CREATOR);
        String shouldName =  I18nServer.getTranslationDefaultResourcebundle(i18Key);
        if(!currentName.equals(shouldName)){
            logger.info("will rename userhome folder user: " + creator+" currentName:"+currentName+" newName:"+shouldName + " scope:"+scope+" execute:"+execute);
            if(execute) {
                try {
                    serviceRegistry.getNodeService().setProperty(nodeRef, ContentModel.PROP_NAME, shouldName);
                    new RepositoryCache().remove(nodeRef.getId());
                }catch (DuplicateChildNodeNameException e){
                    logger.error(e.getMessage());
                }
            }
        }
    }

    @Override
    public Class[] getJobClasses() {
        return super.getJobClasses();
    }
}
