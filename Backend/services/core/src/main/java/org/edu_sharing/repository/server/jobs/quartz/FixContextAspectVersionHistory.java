package org.edu_sharing.repository.server.jobs.quartz;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.cmr.version.VersionHistory;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.jobs.helper.NodeRunner;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@JobDescription(description = "add educontext aspect to versionhistory node")
public class FixContextAspectVersionHistory extends AbstractJob{

    ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
    ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
    NodeService nodeService = serviceRegistry.getNodeService();

    Logger logger = Logger.getLogger(FixContextAspectVersionHistory.class);

    @JobFieldDescription(description = "if just one node needs to be fixed")
    String nodeId;

    @JobFieldDescription(description = "if false (default) no changes will be done.")
    boolean execute;

    @JobFieldDescription(description = "folder to start from")
    String folderId;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {

        nodeId = (String) jobExecutionContext.getJobDetail().getJobDataMap().get("nodeId");
        execute = new Boolean( (String) jobExecutionContext.getJobDetail().getJobDataMap().get("execute"));
        folderId = (String) jobExecutionContext.getJobDetail().getJobDataMap().get("folderId");
        logger.info("nodeId:"+nodeId+" execute:"+execute+" folderId:"+folderId);
        if (nodeId != null) {
            AuthenticationUtil.RunAsWork<Void> runAs = new AuthenticationUtil.RunAsWork<Void>() {
                @Override
                public Void doWork() throws Exception {
                    execute(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId));
                    return null;
                }
            } ;
            AuthenticationUtil.runAsSystem(runAs);
        }else{
            NodeRunner runner = new NodeRunner();
            runner.setTask((ref)->{
                execute(ref);
            });
            runner.setTypes(Collections.singletonList(CCConstants.CCM_TYPE_IO));
            runner.setRunAsSystem(true);
            runner.setThreaded(false);
            runner.setKeepModifiedDate(true);
            runner.setTransaction(NodeRunner.TransactionMode.Local);
            if(folderId != null){
                runner.setStartFolder(folderId);
            }
            int count=runner.run();
        }
    }

    public void execute(NodeRef nodeRef){
        if(!QName.createQName(CCConstants.CCM_TYPE_IO).equals(nodeService.getType(nodeRef))){
            return;
        }
        VersionHistory versionHistory = serviceRegistry.getVersionService().getVersionHistory(nodeRef);
        if(versionHistory == null){
            return;
        }
        for(Version v : versionHistory.getAllVersions()){
            String storeIdentifier = (String)v.getVersionProperty("store-identifier");
            String storeProtocol = (String)v.getVersionProperty("store-protocol");
            String nodeUuid = (String)v.getVersionProperty("node-uuid");
            NodeRef nodeServiceVersionRef = new NodeRef(new StoreRef(storeProtocol,storeIdentifier),nodeUuid);
            logger.info("versioninfo:"+v.getVersionProperties().get(CCConstants.LOM_PROP_LIFECYCLE_VERSION) +" frozenStateNodeRef:"+v.getFrozenStateNodeRef() +" versionedNodeRef:"+v.getVersionedNodeRef() +" nodeServiceVersionRef:"+nodeServiceVersionRef);

            for(Map.Entry entry : v.getVersionProperties().entrySet()){
                logger.debug("versionProp:" + entry.getKey() +": "+entry.getValue());
            }

            if(!nodeService.getAspects(v.getFrozenStateNodeRef()).contains(QName.createQName(CCConstants.CCM_ASPECT_EDUCONTEXT))){
                logger.info(v.getVersionedNodeRef() + " fixing version:" +v.getVersionLabel() + " adding educontext aspect");


                /**
                 * leads to: java.lang.UnsupportedOperationException: This operation is not supported by a version store implementation of the node service
                 * nodeService.addAspect(v.getFrozenStateNodeRef(),QName.createQName(CCConstants.CCM_ASPECT_EDUCONTEXT),null);
                 * so use nodeServiceVersionRef: workspace://version2Store/ce6d62ea-9c04-42d1-b028-ee1aad9e2feb
                 */
                if(execute) {
                    logger.info("fix persists");
                    nodeService.addAspect(nodeServiceVersionRef, QName.createQName(CCConstants.CCM_ASPECT_EDUCONTEXT), null);
                }
            }


            logger.debug("aspects:" + nodeService.getAspects(v.getFrozenStateNodeRef()));
            List<ChildAssociationRef> versionedChildren = this.nodeService.getChildAssocs(v.getFrozenStateNodeRef());
            for (ChildAssociationRef versionedChild : versionedChildren){
                logger.debug("child: " + nodeService.getType(versionedChild.getChildRef()) + " aspects:" +nodeService.getAspects(versionedChild.getChildRef()));
            }
        }

    }
}
