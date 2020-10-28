package org.edu_sharing.repository.server.jobs.helper;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

public class NodeHelper {

    Logger logger = Logger.getLogger(NodeHelper.class);

    ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
    ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);

    public HashMap<String, NodeRef> getImportedNodes(String startFolder) {
        HashMap<String, NodeRef> result = new HashMap<>();
        int processNodes = runTask(startFolder,(ref) -> {
            String replicationSourceId = (String) serviceRegistry.getNodeService().getProperty(ref, QName.createQName(CCConstants.CCM_PROP_IO_REPLICATIONSOURCEID));
            if (replicationSourceId != null) {
                result.put(replicationSourceId, ref);
            }});
        logger.info("processed nodes:" + processNodes + " size:" + result.size());
        return result;
    }

    public List<NodeRef> getNodes(String startFolder){
        List<NodeRef> result = new ArrayList<>();
        int processNodes = runTask(startFolder,(ref) -> { result.add(ref);});
        logger.info("processed nodes:" + processNodes + " size:" + result.size());
        return result;
    }

    private int runTask(String startFolder, Consumer<NodeRef> task) {

        NodeRunner runner = new NodeRunner();
        runner.setTask(task);
        runner.setTypes(Collections.singletonList(CCConstants.CCM_TYPE_IO));
        runner.setStartFolder(startFolder);
        runner.setRunAsSystem(true);
        runner.setTransaction(NodeRunner.TransactionMode.Local);
        runner.setThreaded(false);

        int processNodes = runner.run();
        return processNodes;
    }
}
