/**
 *
 */
package org.edu_sharing.repository.update;

import java.io.Serializable;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.update.UpdateRoutine;
import org.edu_sharing.repository.server.update.UpdateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

@Slf4j
@UpdateService
public class Licenses2 {

    private final static StoreRef storeRef = new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore");

    @UpdateRoutine(
            id = "Licenses2",
            description = "Alle CC* Lizensen die sich in '{http://www.campuscontent.de/model/1.0}assignedlicense' befinden, nach property {http://www.campuscontent.de/model/1.0}commonlicense_key schreiben",
            order = 1001
    )
    public void execute(boolean test) {
        ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
        ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
        SearchService searchService = serviceRegistry.getSearchService();
        NodeService nodeService = serviceRegistry.getNodeService();

        // go thru all IOs which have direct properties
        String searchString = "TYPE:\"{http://www.campuscontent.de/model/1.0}io\" AND @ccm\\:assignedlicense:\"CC*\"";
        ResultSet resultSet = searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, searchString);
        log.info("found " + resultSet.length() + " IO's with direct assignedlicense property");
        int counter = 0;
        for (NodeRef nodeRef : resultSet.getNodeRefs()) {
            Serializable propValue = nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_ASSIGNED_LICENSE_ASSIGNEDLICENSE));
            String nodeType = nodeService.getType(nodeRef).toString();
            log.info("");
            log.info("****************************************************");
            log.info("NodeId:" + nodeRef.getId() + " nodeType:" + nodeType);
            log.info("****************************************************");
            if (nodeType.equals(CCConstants.CCM_TYPE_IO)) {
                if (propValue instanceof List) {
                    List propValueList = (List) propValue;
                    String value = (String) propValueList.get(0);
                    if (value.contains("CC_")) {
                        counter++;
                        log.info("setting property " + CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY + " to " + value);
                        if (!test)
                            nodeService.setProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY), value);
                    }
                } else {
                    log.info("propValue was no list ther is something wrong");
                }
            } else {
                log.info("NOT AN IO: NodeId:" + nodeRef.getId() + " nodeType:" + nodeType);
            }
        }
        log.info(counter + " IO's changed.");
        if (!test) {
            log.info(" Update ends");
        } else {
            log.info(" Test ends");
        }
    }

}
