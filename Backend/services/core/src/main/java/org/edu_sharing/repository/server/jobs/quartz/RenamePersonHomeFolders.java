package org.edu_sharing.repository.server.jobs.quartz;

import lombok.extern.slf4j.Slf4j;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.DuplicateChildNodeNameException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.jobs.helper.NodeRunner;
import org.edu_sharing.repository.server.tools.I18nServer;
import org.edu_sharing.repository.server.tools.cache.RepositoryCache;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

import java.util.List;

@Slf4j
public class RenamePersonHomeFolders extends AbstractJob{

    public static final String PARAM_EXECUTE = "EXECUTE";
    public static final String DESCRIPTION = "Rename folders in person homedir i.e when system language changes.";

    ApplicationContext appContext = AlfAppContextGate.getApplicationContext();
    ServiceRegistry serviceRegistry = (ServiceRegistry)appContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
    Repository repo = (Repository)appContext.getBean("repositoryHelper");
    RepositoryCache repositoryCache = appContext.getBean(RepositoryCache.class);


    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        boolean execute = Boolean.parseBoolean((String)jobExecutionContext.getJobDetail().getJobDataMap().get(PARAM_EXECUTE));
        NodeRunner nr = new NodeRunner();
        nr.setTypes(List.of(CCConstants.CCM_TYPE_MAP));
        nr.setStartFolder(repo.getCompanyHome().getId());
        nr.setRunAsSystem(true);
        nr.setKeepModifiedDate(true);
        nr.setTransaction(NodeRunner.TransactionMode.Local);
        nr.setThreaded(false);
        nr.setTask((NodeRef nodeRef) -> {
            String mapType = (String)serviceRegistry.getNodeService().getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_MAP_TYPE));
            if(mapType == null) return;
            switch (mapType) {
                case CCConstants.CCM_VALUE_MAP_TYPE_DOCUMENTS:
                    rename(nodeRef, CCConstants.CCM_VALUE_MAP_TYPE_DOCUMENTS, CCConstants.I18n_USERFOLDER_DOCUMENTS, execute);
                    break;
                case CCConstants.CCM_VALUE_MAP_TYPE_EDUGROUP:
                    rename(nodeRef, CCConstants.CCM_VALUE_MAP_TYPE_EDUGROUP, CCConstants.I18n_USERFOLDER_GROUPS, execute);
                    break;
                case CCConstants.CCM_VALUE_MAP_TYPE_FAVORITE:
                    rename(nodeRef, CCConstants.CCM_VALUE_MAP_TYPE_FAVORITE, CCConstants.I18n_USERFOLDER_FAVORITES, execute);
                    break;
                case CCConstants.CCM_VALUE_MAP_TYPE_IMAGES:
                    rename(nodeRef, CCConstants.CCM_VALUE_MAP_TYPE_IMAGES, CCConstants.I18n_USERFOLDER_IMAGES, execute);
                    break;
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
            log.info("will rename userhome folder user: {} currentName:{} newName:{} scope:{} execute:{}", creator, currentName, shouldName, scope, execute);
            if(execute) {
                try {
                    serviceRegistry.getNodeService().setProperty(nodeRef, ContentModel.PROP_NAME, shouldName);
                    repositoryCache.remove(nodeRef.getId());
                }catch (DuplicateChildNodeNameException e){
                    log.error(e.getMessage());
                }
            }
        }
    }

    @Override
    public Class<?>[] getJobClasses() {
        return super.getJobClasses();
    }
}
