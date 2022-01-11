package org.edu_sharing.repository.server.jobs.quartz;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.importer.OAIPMHLOMImporter;
import org.edu_sharing.repository.server.jobs.helper.NodeHelper;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

import java.util.*;
import java.util.stream.Collectors;

public class RemoveImportedDuplicates extends AbstractJob{

    public static final String PARAM_START_FOLDER = "START_FOLDER";
    public static final String PARAM_EXECUTE = "EXECUTE";
    public static final String DESCRIPTION = "Find and remove Imported duplicates (got the same replicationsourceid";

    ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
    ServiceRegistry serviceRegistry = (ServiceRegistry)applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
    NodeService nodeService = serviceRegistry.getNodeService();
    BehaviourFilter policyBehaviourFilter = (BehaviourFilter)applicationContext.getBean("policyBehaviourFilter");

    Repository repositoryHelper = (Repository) applicationContext.getBean("repositoryHelper");

    Logger logger = Logger.getLogger(RemoveImportedDuplicates.class);

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        String startFolder = (String)jobExecutionContext.getJobDetail().getJobDataMap().get(PARAM_START_FOLDER);
        Boolean execute = new Boolean((String)jobExecutionContext.getJobDetail().getJobDataMap().get(PARAM_EXECUTE));
        AuthenticationUtil.runAsSystem(() -> {
                excecute(startFolder,execute);
                return null;
            }
        );
    }

    private void excecute(String startFolder, boolean execute){
        if(startFolder == null || startFolder.trim().equals("")){
            for(ChildAssociationRef ref : nodeService.getChildAssocs(repositoryHelper.getCompanyHome())){
                if(OAIPMHLOMImporter.FOLDER_NAME_IMPORTED_OBJECTS.equals(nodeService.getProperty(ref.getChildRef(), ContentModel.PROP_NAME))){
                    startFolder = ref.getChildRef().getId();
                }
            }
            if(startFolder == null){
                logger.error("no imported objects folder found");
                return;
            }
        }

        Map<String, List<NodeRef>> duplicates = new NodeHelper().getDuplicatedImportedNodes(startFolder);
        logger.info("found "+ duplicates.size() +" duplicates in import folder  " + startFolder );

        for(Map.Entry<String, List<NodeRef>> entry : duplicates.entrySet()){
            logger.info(entry.getKey() + ": found duplicates: " + entry.getValue().stream().map(Object::toString).collect(Collectors.joining(",")));
            NodeRef oldestNodeRef = null;
            Date oldest = null;
            for(NodeRef nodeRef : entry.getValue()){
                Date created = (Date)nodeService.getProperty(nodeRef,ContentModel.PROP_CREATED);
                if(oldest == null || oldest.getTime() > created.getTime()) {
                    oldest = (Date) nodeService.getProperty(nodeRef, ContentModel.PROP_CREATED);
                    oldestNodeRef = nodeRef;
                }
            }

            List<ChildAssociationRef> children = nodeService.getChildAssocs(oldestNodeRef, new HashSet(Arrays.asList(new QName[]{
                    QName.createQName(CCConstants.CCM_TYPE_USAGE)})));
            if(children != null && children.size() > 0){
                logger.error(entry.getKey() + ": can not remove " + oldestNodeRef + " cause of usages " + children);
                continue;
            }
            List<String> curriculum = (List<String>)nodeService.getProperty(oldestNodeRef,QName.createQName(CCConstants.getValidGlobalName("ccm:curriculum")));
            if(curriculum != null && curriculum.size() > 0){
                logger.error(entry.getKey() + ": can not remove " + oldestNodeRef + " cause of ccm:curriculum: " + String.join(",",curriculum));
                continue;
            }
            logger.info(entry.getKey()+": would delete oldest:" + oldestNodeRef);
            if(execute){
                NodeRef fNodeRef = oldestNodeRef;
                nodeService.addAspect(fNodeRef, ContentModel.ASPECT_TEMPORARY, null);
                nodeService.deleteNode(fNodeRef);

            }
        }
    }
}
