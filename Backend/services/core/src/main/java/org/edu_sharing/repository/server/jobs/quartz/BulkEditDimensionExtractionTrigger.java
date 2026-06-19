package org.edu_sharing.repository.server.jobs.quartz;

import lombok.extern.slf4j.Slf4j;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.transform.TransformService;
import org.edu_sharing.spring.ApplicationContextFactory;

import java.io.Serializable;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
public class BulkEditDimensionExtractionTrigger implements Consumer<NodeRef> {
    static QName WIDTH = QName.createQName(CCConstants.CCM_PROP_IO_WIDTH);
    static QName HEIGHT = QName.createQName(CCConstants.CCM_PROP_IO_HEIGHT);

    ServiceRegistry serviceRegistry = (ServiceRegistry)AlfAppContextGate.getApplicationContext().getBean(ServiceRegistry.SERVICE_REGISTRY);
    NodeService nodeService = serviceRegistry.getNodeService();
    TransformService transformService = ApplicationContextFactory.getApplicationContext().getBean(TransformService.class);

    @Override
    public void accept(NodeRef nodeRef) {
        exec(nodeRef);
    }

    private void exec(NodeRef nodeRef) {
        log.info("trigger Metadata extraction for node: " + nodeRef);
        Map<String, Serializable> result  = transformService.transform(nodeRef, "alfresco-metadata-extract", Map.class);
        if(result.containsKey(CCConstants.CCM_PROP_IO_WIDTH))
            nodeService.setProperty(nodeRef, WIDTH, result.get(CCConstants.CCM_PROP_IO_WIDTH));
        if(result.containsKey(CCConstants.CCM_PROP_IO_HEIGHT))
            nodeService.setProperty(nodeRef, HEIGHT, result.get(CCConstants.CCM_PROP_IO_HEIGHT));
    }
}
